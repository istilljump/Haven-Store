package com.example.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.dto.AdminMessageSendDTO;
import com.example.admin.dto.AdminResetPasswordDTO;
import com.example.admin.dto.AdminUserCreateDTO;
import com.example.admin.dto.AdminUserUpdateDTO;
import com.example.admin.dto.CategoryFormDTO;
import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.vo.AdminCommentVO;
import com.example.admin.vo.AdminMessageVO;
import com.example.admin.vo.AdminOrderVO;
import com.example.admin.vo.AdminProductVO;
import com.example.admin.vo.AdminUserVO;
import com.example.admin.vo.DashboardVO;
import com.example.category.entity.Category;

import java.util.List;

/**
 * 管理后台业务逻辑接口
 * <p>
 * 覆盖管理后台六个页面（数据概览、用户管理、商品管理、分类管理、系统消息、系统设置）所需的全部能力
 *
 * @author ZCode
 * @date 2026/09/22
 */
public interface AdminService {

    /**
     * 分页查询用户列表（支持关键词与状态过滤）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param keyword  关键词（用户名/昵称/手机号，模糊匹配）
     * @param status   账号状态（1 正常，0 禁用；为空表示不限）
     * @return 用户分页数据
     */
    Page<AdminUserVO> listUsers(Integer page, Integer pageSize, String keyword, Integer status);

    /**
     * 新增用户（管理员操作，可指定是否为管理员）
     *
     * @param dto 新增用户入参
     */
    void createUser(AdminUserCreateDTO dto);

    /**
     * 启用/禁用用户
     *
     * @param userId 用户 ID
     * @param status 目标状态（1 正常，0 禁用）
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 编辑用户资料（昵称、手机号、角色）
     * <p>
     * 不允许管理员把自己的角色改成普通用户，避免把自己锁在后台外
     *
     * @param userId 用户 ID
     * @param dto    编辑入参（null/不传的字段保持不变）
     */
    void updateUser(Long userId, AdminUserUpdateDTO dto);

    /**
     * 重置用户密码（管理员操作）
     * <p>
     * 重置后清除该用户的登录缓存，强制其用新密码重新登录
     *
     * @param userId 用户 ID
     * @param dto    新密码
     */
    void resetPassword(Long userId, AdminResetPasswordDTO dto);

    /**
     * 获取管理后台首页数据
     *
     * @return 统计数据 + 趋势 + 分类分布 + 最近商品 + 最近系统消息
     */
    DashboardVO getDashboard();

    /**
     * 近 N 日新增趋势（用户/商品/订单，按天聚合）
     *
     * @param days 天数（1-30）
     * @return 按日期升序的趋势点列表
     */
    List<DashboardVO.TrendPoint> getTrend(Integer days);

    /**
     * 商品分类分布（按分类聚合商品数）
     *
     * @return 分类分布列表（按商品数降序）
     */
    List<DashboardVO.CategoryStat> getCategoryStats();

    // ==================== 订单管理 ====================

    /**
     * 分页查询全平台订单（支持状态与订单号关键词过滤）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param status   订单状态（1 待支付，2 已支付，3 已取消，4 已完成；为空表示不限）
     * @param keyword  关键词（订单号，模糊匹配）
     * @return 订单分页数据（不含明细，明细走详情接口）
     */
    Page<AdminOrderVO> listOrders(Integer page, Integer pageSize, Integer status, String keyword);

    /**
     * 查询订单详情（含明细与买卖双方名称）
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    AdminOrderVO getOrder(String orderNo);

    /**
     * 导出订单数据为 CSV 文本
     *
     * @param status  订单状态（为空表示全部）
     * @param keyword 关键词（订单号）
     * @return CSV 文本
     */
    String exportOrdersCsv(Integer status, String keyword);

    // ==================== 评论管理 ====================

    /**
     * 分页查询全平台评论（支持商品与状态过滤）
     *
     * @param page      页码
     * @param pageSize  每页条数
     * @param productId 商品 ID（为空表示不限）
     * @param status    评论状态（1 正常，0 隐藏；为空表示不限）
     * @return 评论分页数据
     */
    Page<AdminCommentVO> listComments(Integer page, Integer pageSize, Long productId, Integer status);

    /**
     * 变更评论状态（显示 / 隐藏）
     *
     * @param commentId 评论 ID
     * @param status    目标状态（1 正常，0 隐藏）
     */
    void updateCommentStatus(Long commentId, Integer status);

    /**
     * 删除单条评论（物理删除）
     *
     * @param commentId 评论 ID
     */
    void deleteComment(Long commentId);

    /**
     * 分页查询商品列表（支持关键词、状态、分类过滤）
     *
     * @param page       页码
     * @param pageSize   每页条数
     * @param keyword    关键词（商品标题，模糊匹配）
     * @param status     商品状态（1 在售，2 已售出，3 已下架；为空表示不限）
     * @param categoryId 分类 ID（为空表示不限）
     * @return 商品分页数据
     */
    Page<AdminProductVO> listProducts(Integer page, Integer pageSize, String keyword,
                                     Integer status, Integer categoryId);

    /**
     * 变更商品状态（下架 / 重新上架）
     *
     * @param productId 商品 ID
     * @param status    目标状态（1 在售，2 已售出，3 已下架）
     */
    void updateProductStatus(Long productId, Integer status);

    /**
     * 删除商品
     * <p>
     * 物理删除，并统一清理商品图片、评论与收藏/购物车关系（复用商品模块的级联删除）
     *
     * @param productId 商品 ID
     */
    void deleteProduct(Long productId);

    /**
     * 查询全部分类（按排序值升序）
     *
     * @return 分类列表
     */
    List<Category> listCategories();

    /**
     * 新增分类
     *
     * @param dto 分类表单
     */
    void addCategory(CategoryFormDTO dto);

    /**
     * 修改分类
     *
     * @param categoryId 分类 ID
     * @param dto        分类表单
     */
    void updateCategory(Integer categoryId, CategoryFormDTO dto);

    /**
     * 删除分类（分类下存在商品时拒绝删除）
     *
     * @param categoryId 分类 ID
     */
    void deleteCategory(Integer categoryId);

    /**
     * 分页查询系统消息（按公告聚合，一条群发记录合并为一行）
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @param userType 接收群体（all/admin/user；为空表示不限）
     * @param status   消息状态（1 发送中，2 已送达；为空表示不限）
     * @return 系统消息分页数据
     */
    Page<AdminMessageVO> listMessages(Integer page, Integer pageSize, String userType, Integer status);

    /**
     * 群发系统消息
     *
     * @param dto 群发入参
     */
    void sendMessage(AdminMessageSendDTO dto);

    /**
     * 删除系统消息（按公告聚合删除）
     * <p>
     * 群发时一条公告会按接收者展开为多行，删除时整组一起删，
     * 否则会出现「删了一条还剩几十条」的怪现象
     *
     * @param messageId 消息 ID（同组中任意一条即可定位该公告）
     */
    void deleteMessage(Long messageId);

    /**
     * 获取系统设置（未保存过时返回默认值）
     *
     * @return 系统设置
     */
    SystemSettingDTO getSettings();

    /**
     * 保存系统设置
     *
     * @param dto 系统设置
     */
    void updateSettings(SystemSettingDTO dto);

    /**
     * 导出用户数据为 CSV 文本
     *
     * @param keyword 关键词（用户名/昵称/手机号）
     * @param status  账号状态（1 正常，0 禁用；为空表示不限）
     * @return CSV 文本（不含表头 BOM，由控制器补充）
     */
    String exportUsersCsv(String keyword, Integer status);

    /**
     * 导出商品数据为 CSV 文本
     *
     * @param keyword    关键词（商品标题）
     * @param status     商品状态
     * @param categoryId 分类 ID
     * @return CSV 文本（不含表头 BOM，由控制器补充）
     */
    String exportProductsCsv(String keyword, Integer status, Integer categoryId);
}
