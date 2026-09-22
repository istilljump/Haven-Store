package com.example.product.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.category.entity.Category;
import com.example.common.Result;
import com.example.product.dto.CommentAddDTO;
import com.example.product.dto.ProductAddDTO;
import com.example.product.dto.ProductNearbyQueryDTO;
import com.example.product.vo.CommentVO;
import com.example.product.vo.ProductAddVO;
import com.example.product.vo.ProductDetailVO;
import com.example.product.vo.ProductListVO;
import com.example.product.vo.ProductNearbyVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 二手商品模块业务逻辑接口
 *
 * @author ZCode
 * @date 2026/09/21
 */
public interface ProductService {

    /**
     * 发布二手商品
     * <p>
     * 卖家用户 ID 从登录态解析（禁止前端传入），商品保存后默认为上架状态
     *
     * @param dto 发布入参（标题、描述、分类、价格、成色、交易方式、地址、经纬度）
     * @return 新发布商品的 ID
     */
    Result<ProductAddVO> addProduct(ProductAddDTO dto);

    /**
     * 查询附近商品
     * <p>
     * 根据经纬度查询指定半径内的商品，按距离排序
     *
     * @param dto 查询参数（中心点经纬度、查询半径、分页参数）
     * @return 附近商品列表
     */
    Result<Page<ProductNearbyVO>> findNearbyProducts(ProductNearbyQueryDTO dto);

    /**
     * 查询可用的商品分类
     * <p>
     * 只返回启用状态的分类（管理后台禁用后的分类不再出现在选择列表中），按排序值升序
     *
     * @return 可用分类列表
     */
    List<Category> listEnabledCategories();

    /**
     * 搜索在售商品（前台商品列表页）
     * <p>
     * 只返回在售状态（status=1）的商品；关键词对标题模糊匹配
     *
     * @param keyword    关键词（商品标题，可为空）
     * @param categoryId 分类 ID（可为空）
     * @param minPrice   价格下限（可为空）
     * @param maxPrice   价格上限（可为空）
     * @param sort       排序方式：latest 最新发布（默认）/ priceAsc 价格升序 / priceDesc 价格降序
     * @param page       页码
     * @param pageSize   每页条数
     * @return 商品分页数据
     */
    Page<ProductListVO> searchProducts(String keyword, Integer categoryId, BigDecimal minPrice,
                                       BigDecimal maxPrice, String sort, Integer page, Integer pageSize);

    /**
     * 获取商品详情（前台商品详情页）
     * <p>
     * 每次调用会把该商品的浏览次数 +1
     *
     * @param productId 商品 ID
     * @return 商品详情
     */
    ProductDetailVO getProductDetail(Long productId);

    /**
     * 收藏商品
     *
     * @param productId 商品 ID
     */
    void addFavorite(Long productId);

    /**
     * 取消收藏商品
     *
     * @param productId 商品 ID
     */
    void removeFavorite(Long productId);

    /**
     * 查询当前用户收藏的商品
     *
     * @param page     页码
     * @param pageSize 每页条数
     * @return 商品分页数据
     */
    Page<ProductListVO> listMyFavorites(Integer page, Integer pageSize);

    /**
     * 查询当前用户发布的商品（我的发布）
     *
     * @param status   商品状态（1 在售，2 已售出，3 已下架；为空表示全部）
     * @param page     页码
     * @param pageSize 每页条数
     * @return 商品分页数据
     */
    Page<ProductListVO> listMyProducts(Integer status, Integer page, Integer pageSize);

    /**
     * 修改自己发布的商品
     *
     * @param productId 商品 ID
     * @param dto       修改入参（仅发布者本人可改）
     */
    void updateProduct(Long productId, ProductAddDTO dto);

    /**
     * 下架自己发布的商品（软下架：状态改为已下架，不删数据）
     *
     * @param productId 商品 ID
     */
    void offlineProduct(Long productId);

    /**
     * 分页查询商品评论（游客可访问）
     *
     * @param productId 商品 ID
     * @param page      页码
     * @param pageSize  每页条数
     * @return 评论分页数据
     */
    Page<CommentVO> listComments(Long productId, Integer page, Integer pageSize);

    /**
     * 发表商品评论
     * <p>
     * 需要登录；不允许评价自己发布的商品；同一用户对同一商品只能评价一次
     *
     * @param productId 商品 ID
     * @param dto       评论入参
     */
    void addComment(Long productId, CommentAddDTO dto);
}
