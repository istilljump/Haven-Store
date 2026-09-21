package com.example.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BusinessException;
import com.example.common.RedisKeyConst;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.user.constant.UserConstant;
import com.example.user.dto.LoginDTO;
import com.example.user.dto.RegisterDTO;
import com.example.user.entity.User;
import com.example.user.enums.UserStatusEnum;
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserService;
import com.example.user.vo.LoginUserVO;
import com.example.utils.JwtUtil;
import com.example.utils.PasswordUtil;
import com.example.utils.RedisUtil;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户模块业务逻辑实现类
 * <p>
 * 说明：查询全部通过 MyBatis-Plus LambdaQueryWrapper 构建，参数预编译，无 SQL 注入风险；
 * 密码明文只参与加密与比对，不写入日志、不出现在任何返回结果中
 *
 * @author ZCode
 * @date 2026/09/21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 用户模块数据访问对象 */
    private final UserMapper userMapper;

    /** 密码加密工具 */
    private final PasswordUtil passwordUtil;

    /** JWT 工具 */
    private final JwtUtil jwtUtil;

    /** Redis 操作工具 */
    private final RedisUtil redisUtil;

    /** Token 有效期（毫秒，从配置文件读取），用于同步设置用户信息缓存的过期时间 */
    @Value("${jwt.expiration}")
    private Long tokenExpiration;

    /**
     * 用户注册：
     * 校验两次密码一致性与用户名、手机号唯一性，密码 BCrypt 加密后写入数据库
     *
     * @param dto 注册入参
     * @return 注册结果
     */
    @Override
    public Result<Void> register(RegisterDTO dto) {
        // 1. 校验两次输入的密码是否一致（跨字段校验无法通过标准校验注解实现，须在业务层校验）
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        // 2. 校验用户名唯一
        Long usernameCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (usernameCount != null && usernameCount > 0) {
            throw new BusinessException("用户名已被注册");
        }
        // 3. 校验手机号唯一
        Long phoneCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, dto.getPhone()));
        if (phoneCount != null && phoneCount > 0) {
            throw new BusinessException("该手机号已被注册");
        }
        // 4. 构建用户实体：密码 BCrypt 加密存储，设置默认头像与正常状态
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordUtil.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getNickname());
        user.setAvatar(UserConstant.DEFAULT_AVATAR);
        user.setStatus(UserStatusEnum.ENABLE.getCode());
        // 5. 写入数据库（创建时间、更新时间由 MyBatis-Plus 自动填充）
        userMapper.insert(user);
        log.info("用户注册成功，用户ID：{}，用户名：{}", user.getId(), user.getUsername());
        return Result.success();
    }

    /**
     * 用户登录：
     * 根据用户名查询用户，校验账号状态与密码，生成 JWT Token 并缓存用户信息到 Redis
     *
     * @param dto 登录入参
     * @return 登录用户信息（含 Token）
     */
    @Override
    public Result<LoginUserVO> login(LoginDTO dto) {
        // 1. 根据用户名查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        // 用户不存在：统一提示，不暴露“账号不存在”细节，防止账号枚举
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        // 2. 校验账号状态
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        // 3. 密码比对（BCrypt 密文比对，全程不记录密码内容）
        if (!passwordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId());
        // 5. 缓存用户信息到 Redis，过期时间与 Token 有效期保持一致（后续可用于强制下线等场景）
        redisUtil.set(RedisKeyConst.USER_INFO_KEY + user.getId(), user, tokenExpiration, TimeUnit.MILLISECONDS);
        // 6. 封装脱敏的登录返回信息（不含密码、手机号等敏感字段）
        LoginUserVO loginUserVO = LoginUserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .token(token)
                .build();
        log.info("用户登录成功，用户ID：{}", user.getId());
        return Result.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息（脱敏）：
     * 优先读取 Redis 缓存，缓存未命中时回源数据库并重建缓存
     *
     * @return 当前登录用户信息（密码字段已置空）
     */
    @Override
    public Result<User> getCurrentUserInfo() {
        // 1. 从线程上下文获取当前登录用户 ID（由 JWT 拦截器写入，不从请求参数获取，杜绝越权）
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        // 2. 优先读取 Redis 缓存
        User user = redisUtil.get(RedisKeyConst.USER_INFO_KEY + userId);
        if (user == null) {
            // 3. 缓存未命中，回源数据库查询
            user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
                throw new BusinessException("账号已被禁用，请联系管理员");
            }
            // 4. 重建缓存，过期时间与 Token 有效期保持一致
            redisUtil.set(RedisKeyConst.USER_INFO_KEY + userId, user, tokenExpiration, TimeUnit.MILLISECONDS);
        }
        // 5. 密码脱敏：任何返回结果都不允许出现密码字段
        user.setPassword(null);
        return Result.success(user);
    }
}
