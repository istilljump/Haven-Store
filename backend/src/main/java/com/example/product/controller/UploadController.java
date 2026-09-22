package com.example.product.controller;

import com.example.common.BusinessException;
import com.example.common.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 文件上传控制器（当前仅支持商品图片）
 * <p>
 * 说明：图片直接落盘到本地上传目录，通过 WebMvcConfig 注册的 /uploads/** 静态映射对外提供访问。
 * 返回的是「可直接拼进 img src」的地址（含 /api 上下文路径），前端不需要再做拼接。
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Slf4j
@Api(tags = "文件上传接口")
@RestController
@RequestMapping("/product")
public class UploadController {

    /** 允许的图片扩展名（按实际内容探测，不信任客户端文件名） */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /** 单张图片大小上限：5MB */
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    /** 上传根目录（从配置文件读取，默认 ./uploads） */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** 静态资源访问前缀（与 WebMvcConfig 中的映射保持一致） */
    @Value("${file.access-prefix:/api/uploads}")
    private String accessPrefix;

    /**
     * 上传商品图片
     *
     * @param file 图片文件
     * @return 可直接用于 img src 的访问地址
     */
    @ApiOperation(value = "上传商品图片", notes = "需要登录；仅支持 jpg/jpeg/png/gif/webp，单张不超过 5MB")
    @PostMapping("/upload")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("图片大小不能超过 5MB");
        }

        String extension = resolveExtension(file);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg、jpeg、png、gif、webp 格式的图片");
        }

        // 按天分目录，避免单目录文件过多影响查询
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 文件名使用随机 UUID，杜绝客户端文件名带来的路径穿越与覆盖风险
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        Path targetDir = Paths.get(uploadDir, datePath).toAbsolutePath().normalize();
        Path targetFile = targetDir.resolve(fileName);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("图片保存失败，目标路径：{}", targetFile, e);
            throw new BusinessException("图片保存失败，请稍后重试");
        }

        String url = accessPrefix + "/" + datePath + "/" + fileName;
        log.info("图片上传成功，访问地址：{}", url);
        return Result.success(url);
    }

    /**
     * 解析图片扩展名
     * <p>
     * 优先取原始文件名后缀；同时用探测到的内容类型兜底（部分客户端上传时不带文件名后缀）
     */
    private String resolveExtension(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (StringUtils.hasText(original) && original.contains(".")) {
            return original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.contains("/")) {
            return "";
        }
        String sub = contentType.substring(contentType.indexOf('/') + 1).toLowerCase(Locale.ROOT);
        return "jpeg".equals(sub) ? "jpg" : sub;
    }
}
