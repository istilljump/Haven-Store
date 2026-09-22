package com.example.admin.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.admin.dto.AdminMessageSendDTO;
import com.example.admin.dto.AdminUserCreateDTO;
import com.example.admin.dto.CategoryFormDTO;
import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.vo.AdminMessageVO;
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
     * 获取管理后台首页数据
     *
     * @return 统计数据 + 最近商品 + 最近系统消息
     */
    DashboardVO getDashboard();

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
