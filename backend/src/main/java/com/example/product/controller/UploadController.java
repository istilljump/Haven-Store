package com.example.product.controller;

import com.example.common.Result;
import com.example.utils.FileStorageUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器（当前仅支持商品图片）
 * <p>
 * 具体的校验与落盘规则见 {@link FileStorageUtil}；返回的地址已带上下文路径，
 * 前端可直接作为 img src 使用
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Api(tags = "文件上传接口")
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class UploadController {

    /** 图片存储工具 */
    private final FileStorageUtil fileStorageUtil;

    /**
     * 上传商品图片
     *
     * @param file 图片文件
     * @return 可直接用于 img src 的访问地址
     */
    @ApiOperation(value = "上传商品图片", notes = "需要登录；仅支持 jpg/jpeg/png/gif/webp，单张不超过 5MB")
    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(fileStorageUtil.storeImage(file, "product"));
    }
}
