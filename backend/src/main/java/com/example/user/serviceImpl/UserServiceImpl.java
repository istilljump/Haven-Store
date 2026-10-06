package com.example.user.serviceImpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.common.BusinessException;
import com.example.common.RedisKeyConst;
import com.example.common.Result;
import com.example.common.ResultCodeEnum;
import com.example.user.constant.UserConstant;
import com.example.user.dto.LoginDTO;
import com.example.user.dto.ChangePasswordDTO;
import com.example.user.dto.RegisterDTO;
import com.example.user.dto.UserUpdateDTO;
import com.example.user.entity.User;
import com.example.user.enums.UserStatusEnum;
import com.example.user.mapper.UserMapper;
import com.example.user.service.UserService;
import com.example.user.vo.LoginUserVO;
import com.example.user.vo.UserInfoVO;
import com.example.utils.JwtUtil;
import com.example.utils.PasswordUtil;
import com.example.utils.RedisUtil;
import com.example.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    /** 系统设置服务：登录锁定、密码强度、会话时效均从后台设置读取 */
    private final SystemSettingService systemSettingService;

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
        validatePasswordPolicy(dto.getPassword(), "密码");
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordUtil.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getNickname());
        user.setAvatar(UserConstant.DEFAULT_AVATAR);
        user.setStatus(UserStatusEnum.ENABLE.getCode());
        // 注册一律为普通用户，管理员角色只能由数据库或后台赋予
        user.setRole(UserConstant.ROLE_USER);
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
        // 0. 登录锁定校验：失败次数达到后台设置上限后，锁定时长内拒绝登录（防暴力破解）
        assertNotLocked(dto.getUsername());
        // 1. 根据用户名查询用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        // 用户不存在：统一提示，不暴露“账号不存在”细节，防止账号枚举
        if (user == null) {
            recordLoginFailure(dto.getUsername());
            throw new BusinessException("用户名或密码错误");
        }
        // 2. 校验账号状态
        if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }
        // 3. 密码比对（BCrypt 密文比对，全程不记录密码内容）
        if (!passwordUtil.matches(dto.getPassword(), user.getPassword())) {
            recordLoginFailure(dto.getUsername());
            throw new BusinessException("用户名或密码错误");
        }
        // 登录成功：清掉失败计数
        redisUtil.deleteQuietly(RedisKeyConst.LOGIN_FAIL_KEY + dto.getUsername());
        // 4. 生成 JWT Token：会话超时时间在后台设置中启用时以其为准，否则用配置默认值
        String token = jwtUtil.generateToken(user.getId(), resolveSessionTtlMillis());
        // 5. 缓存用户信息到 Redis，过期时间与 Token 有效期保持一致（后续可用于强制下线等场景）
        //    密码字段不落缓存；用安全版本写入：Redis 只是缓存，不可用时登录流程必须照常完成
        user.setPassword(null);
        redisUtil.setQuietly(RedisKeyConst.USER_INFO_KEY + user.getId(), user, tokenExpiration, TimeUnit.MILLISECONDS);
        // 6. 封装脱敏的登录返回信息（不含密码、手机号等敏感字段）
        LoginUserVO loginUserVO = LoginUserVO.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .token(token)
                .isAdmin(UserConstant.ROLE_ADMIN.equals(user.getRole()))
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
    public Result<UserInfoVO> getCurrentUserInfo() {
        // 1. 从线程上下文获取当前登录用户 ID（由 JWT 拦截器写入，不从请求参数获取，杜绝越权）
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        // 2. 优先读取 Redis 缓存（缓存不可用时返回 null，下面会回源数据库）
        User user = redisUtil.getQuietly(RedisKeyConst.USER_INFO_KEY + userId);
        if (user == null) {
            // 3. 缓存未命中，回源数据库查询
            user = userMapper.selectById(userId);
            if (user == null) {
                throw new BusinessException("用户不存在");
            }
            if (!UserStatusEnum.ENABLE.getCode().equals(user.getStatus())) {
                throw new BusinessException("账号已被禁用，请联系管理员");
            }
            // 4. 重建缓存：密码不落缓存，过期时间与 Token 有效期保持一致
            user.setPassword(null);
            redisUtil.setQuietly(RedisKeyConst.USER_INFO_KEY + userId, user, tokenExpiration, TimeUnit.MILLISECONDS);
        }
        // 5. 密码脱敏：任何返回结果都不允许出现密码字段（历史缓存条目可能仍带密码）
        user.setPassword(null);
        return Result.success(toUserInfoVO(user));
    }

    /**
     * 修改当前登录用户的资料
     * <p>
     * 说明：使用 UpdateWrapper 逐字段显式赋值——updateById 会忽略 null 字段，
     * 无法表达「清空邮箱/简介」的语义；这里约定空白字符串即清空该栏
     */
    @Override
    public void updateCurrentUserInfo(UserUpdateDTO dto) {
        Long userId = requireLoginUserId();
        // 手机号填写时需保持唯一（排除自己）
        if (StringUtils.hasText(dto.getPhone())) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .eq(User::getPhone, dto.getPhone())
                    .ne(User::getId, userId));
            if (count != null && count > 0) {
                throw new BusinessException("该手机号已被其他账号使用");
            }
        }

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>().eq(User::getId, userId);
        // 昵称未填写时保持原值不变；手机号/邮箱/简介空白即清空
        if (StringUtils.hasText(dto.getNickname())) {
            wrapper.set(User::getNickname, dto.getNickname());
        }
        wrapper.set(User::getPhone, StringUtils.hasText(dto.getPhone()) ? dto.getPhone() : null);
        wrapper.set(User::getEmail, StringUtils.hasText(dto.getEmail()) ? dto.getEmail() : null);
        wrapper.set(User::getBio, StringUtils.hasText(dto.getBio()) ? dto.getBio() : null);
        userMapper.update(null, wrapper);

        // 资料变更后清掉缓存，避免个人中心读到旧数据
        redisUtil.deleteQuietly(RedisKeyConst.USER_INFO_KEY + userId);
        log.info("用户资料已更新，用户ID：{}", userId);
    }

    /**
     * 修改当前登录用户的密码
     * <p>
     * 校验原密码 → 校验两次新密码一致 → BCrypt 加密后落库；
     * 改密后清除用户缓存，相当于强制重新登录
     */
    @Override
    public void changePassword(ChangePasswordDTO dto) {
        Long userId = requireLoginUserId();
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordUtil.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        validatePasswordPolicy(dto.getNewPassword(), "新密码");

        User update = new User();
        update.setId(userId);
        update.setPassword(passwordUtil.encode(dto.getNewPassword()));
        userMapper.updateById(update);

        redisUtil.deleteQuietly(RedisKeyConst.USER_INFO_KEY + userId);
        log.info("用户密码已修改，用户ID：{}", userId);
    }

    /**
     * 更新当前登录用户的头像
     */
    @Override
    public void updateAvatar(String avatarUrl) {
        Long userId = requireLoginUserId();
        if (!StringUtils.hasText(avatarUrl)) {
            throw new BusinessException("头像地址不能为空");
        }
        User update = new User();
        update.setId(userId);
        update.setAvatar(avatarUrl);
        userMapper.updateById(update);

        redisUtil.deleteQuietly(RedisKeyConst.USER_INFO_KEY + userId);
        log.info("用户头像已更新，用户ID：{}", userId);
    }

    /**
     * 取当前登录用户 ID，未登录直接抛 401
     */
    private Long requireLoginUserId() {
        Long userId = UserHolder.getUserId();
        if (userId == null) {
            throw new BusinessException(ResultCodeEnum.UNAUTHORIZED);
        }
        return userId;
    }

    /**
     * 加载用于鉴权的用户实体（Redis 缓存优先，未命中回源数据库并重建缓存）
     * <p>
     * 供 JWT 拦截器在每个请求上校验账号状态：禁用账号的 Token 立即失效，
     * 不再出现「禁用后 7 天内仍可下单发帖」的窗口期。
     * 密码字段不参与缓存，也不会随本方法外泄
     */
    @Override
    public User getAuthUser(Long userId) {
        User user = redisUtil.getQuietly(RedisKeyConst.USER_INFO_KEY + userId);
        if (user == null) {
            user = userMapper.selectById(userId);
            if (user != null) {
                user.setPassword(null);
                redisUtil.setQuietly(RedisKeyConst.USER_INFO_KEY + userId, user, tokenExpiration, TimeUnit.MILLISECONDS);
            }
        }
        return user;
    }

    /**
     * 校验账号是否处于登录锁定状态（防暴力破解）
     * <p>
     * 锁定 Key 由 {@link #recordLoginFailure} 在失败次数达到上限时写入；
     * Redis 不可用时按未锁定处理，不影响正常登录
     */
    private void assertNotLocked(String username) {
        SystemSettingDTO.SecuritySetting security = currentSecuritySetting();
        int lockMinutes = security == null || security.getLockDuration() == null
                ? 30 : security.getLockDuration();
        if (redisUtil.hasKeyQuietly(RedisKeyConst.LOGIN_LOCK_KEY + username)) {
            throw new BusinessException("登录失败次数过多，账号已锁定，请 " + lockMinutes + " 分钟后再试");
        }
    }

    /**
     * 记录一次登录失败：计数达到后台设置上限时写入锁定 Key
     */
    private void recordLoginFailure(String username) {
        SystemSettingDTO.SecuritySetting security = currentSecuritySetting();
        int attempts = security == null || security.getLoginAttempts() == null
                ? 5 : security.getLoginAttempts();
        int lockMinutes = security == null || security.getLockDuration() == null
                ? 30 : security.getLockDuration();
        Long count = redisUtil.incrementQuietly(RedisKeyConst.LOGIN_FAIL_KEY + username, lockMinutes, TimeUnit.MINUTES);
        if (count != null && count >= attempts) {
            // 达到上限：写入锁定 Key 并清零计数（锁定 Key 自带过期时间，到期自动解锁）
            redisUtil.setQuietly(RedisKeyConst.LOGIN_LOCK_KEY + username, 1, lockMinutes, TimeUnit.MINUTES);
            redisUtil.deleteQuietly(RedisKeyConst.LOGIN_FAIL_KEY + username);
            log.warn("账号登录失败次数达到上限，已临时锁定，用户名：{}，锁定时长：{} 分钟", username, lockMinutes);
        }
    }

    /**
     * 解析会话有效期（毫秒）：安全设置启用会话超时时以其为准，否则用配置默认值
     */
    private long resolveSessionTtlMillis() {
        SystemSettingDTO.SecuritySetting security = currentSecuritySetting();
        Integer timeoutMinutes = security == null ? null : security.getSessionTimeout();
        if (timeoutMinutes != null && timeoutMinutes > 0) {
            return timeoutMinutes * 60L * 1000L;
        }
        return tokenExpiration;
    }

    /**
     * 校验密码强度（依据后台「安全设置」的 passwordStrength）
     * <ul>
     *   <li>low：仅要求非空（长度校验由 DTO 注解完成）</li>
     *   <li>medium：须同时包含字母与数字</li>
     *   <li>high：长度不少于 8 位，且同时包含大写字母、小写字母与数字</li>
     * </ul>
     *
     * @param password 待校验的明文密码
     * @param label    提示语中的字段名（密码/新密码）
     */
    private void validatePasswordPolicy(String password, String label) {
        if (!StringUtils.hasText(password)) {
            return;
        }
        SystemSettingDTO.SecuritySetting security = currentSecuritySetting();
        String strength = security == null || !StringUtils.hasText(security.getPasswordStrength())
                ? "medium" : security.getPasswordStrength();
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        if ("high".equals(strength)) {
            boolean hasUpper = password.matches(".*[A-Z].*");
            boolean hasLower = password.matches(".*[a-z].*");
            if (password.length() < 8 || !hasUpper || !hasLower || !hasDigit) {
                throw new BusinessException(label + "强度不足：须至少 8 位，且同时包含大写字母、小写字母和数字");
            }
        } else if ("medium".equals(strength)) {
            if (!hasLetter || !hasDigit) {
                throw new BusinessException(label + "强度不足：须同时包含字母和数字");
            }
        }
        // low：不做额外要求
    }

    /**
     * 读取当前安全设置（设置服务内部已兜底默认值，不会返回 null 的 DTO）
     */
    private SystemSettingDTO.SecuritySetting currentSecuritySetting() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings == null ? null : settings.getSecurity();
    }

    /**
     * 用户实体 -> 个人中心展示对象（脱敏，并补齐 isAdmin）
     */
    private UserInfoVO toUserInfoVO(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setBio(user.getBio());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setIsAdmin(UserConstant.ROLE_ADMIN.equals(user.getRole()));
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
