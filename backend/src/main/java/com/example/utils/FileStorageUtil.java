package com.example.utils;

import com.example.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
 * 图片存储工具
 * <p>
 * 商品图片与用户头像共用同一套落盘规则：按「业务分类 / 日期」分目录，
 * 文件名使用随机 UUID（不使用客户端文件名，杜绝路径穿越与同名覆盖），
 * 返回带上下文路径的可直接访问地址。
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Slf4j
@Component
public class FileStorageUtil {

    /** 允许的图片扩展名 */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /** 单张图片大小上限：5MB */
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    /** 上传根目录（从配置文件读取） */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** 静态资源访问前缀（含上下文路径） */
    @Value("${file.access-prefix:/api/uploads}")
    private String accessPrefix;

    /**
     * 保存图片并返回可访问地址
     *
     * @param file     上传的图片
     * @param category 业务分类目录，如 product / avatar
     * @return 可直接用于 img src 的地址
     */
    public String storeImage(MultipartFile file, String category) {
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

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        Path targetDir = Paths.get(uploadDir, category, datePath).toAbsolutePath().normalize();
        Path targetFile = targetDir.resolve(fileName);
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetFile.toFile());
        } catch (IOException e) {
            log.error("图片保存失败，目标路径：{}", targetFile, e);
            throw new BusinessException("图片保存失败，请稍后重试");
        }

        String url = accessPrefix + "/" + category + "/" + datePath + "/" + fileName;
        log.info("图片上传成功，访问地址：{}", url);
        return url;
    }

    /**
     * 解析图片扩展名
     * <p>
     * 优先取原始文件名后缀；部分客户端不带后缀时，退回用探测到的内容类型
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
