package com.example.utils;

import com.example.admin.dto.SystemSettingDTO;
import com.example.admin.service.SystemSettingService;
import com.example.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 图片存储工具
 * <p>
 * 商品图片与用户头像共用同一套落盘规则：按「业务分类 / 日期」分目录，
 * 文件名使用随机 UUID（不使用客户端文件名，杜绝路径穿越与同名覆盖），
 * 返回带上下文路径的可直接访问地址。
 * <p>
 * 校验规则分两层：
 * 1. 扩展名白名单 + 大小上限来自管理后台「上传设置」（未配置时用内置默认值）；
 * 2. 文件头（魔数）校验：拒绝伪装成图片的脚本或任意二进制内容。
 *
 * @author ZCode
 * @date 2026/09/22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileStorageUtil {

    /** 允许的图片扩展名（上传设置未配置时的默认值） */
    private static final List<String> DEFAULT_ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");

    /** 单张图片大小上限默认值：5MB（上传设置未配置时使用） */
    private static final long DEFAULT_MAX_SIZE_BYTES = 5L * 1024 * 1024;

    /** 上传根目录（从配置文件读取） */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /** 静态资源访问前缀（含上下文路径） */
    @Value("${file.access-prefix:/api/uploads}")
    private String accessPrefix;

    /** 系统设置服务：读取管理后台「上传设置」（大小上限、类型白名单） */
    private final SystemSettingService systemSettingService;

    /**
     * 保存图片并返回可访问地址
     *
     * @param file     上传的图片
     * @param category 业务分类目录，如 product / avatar / settings
     * @return 可直接用于 img src 的地址
     */
    public String storeImage(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的图片");
        }
        // 1. 大小校验（上限来自上传设置，默认 5MB）
        long maxBytes = resolveMaxSizeBytes();
        if (file.getSize() > maxBytes) {
            throw new BusinessException("图片大小不能超过 " + (maxBytes / 1024 / 1024) + "MB");
        }
        // 2. 扩展名白名单校验（白名单来自上传设置）
        String extension = resolveExtension(file);
        if (!resolveAllowedExtensions().contains(extension)) {
            throw new BusinessException("仅支持 " + String.join("、", resolveAllowedExtensions()) + " 格式的图片");
        }
        // 3. 文件头（魔数）校验：扩展名可能是伪装的，内容必须真的是图片
        verifyMagicBytes(file, extension);

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

    /**
     * 读取当前允许的扩展名白名单（管理后台「上传设置」，未配置时回落默认值）
     */
    private List<String> resolveAllowedExtensions() {
        SystemSettingDTO.UploadSetting upload = currentUploadSetting();
        if (upload == null || upload.getAllowedTypes() == null || upload.getAllowedTypes().isEmpty()) {
            return DEFAULT_ALLOWED_EXTENSIONS;
        }
        return upload.getAllowedTypes().stream()
                .filter(StringUtils::hasText)
                .map(t -> t.trim().toLowerCase(Locale.ROOT))
                .map(t -> "jpeg".equals(t) ? "jpg" : t)
                .collect(Collectors.toList());
    }

    /**
     * 读取当前单文件大小上限（字节；上传设置未配置或非法时回落 5MB 默认值）
     */
    private long resolveMaxSizeBytes() {
        SystemSettingDTO.UploadSetting upload = currentUploadSetting();
        Integer maxFileSizeKB = upload == null ? null : upload.getMaxFileSize();
        if (maxFileSizeKB == null || maxFileSizeKB <= 0) {
            return DEFAULT_MAX_SIZE_BYTES;
        }
        return maxFileSizeKB * 1024L;
    }

    /**
     * 读取上传设置（设置服务内部已兜底默认值）
     */
    private SystemSettingDTO.UploadSetting currentUploadSetting() {
        SystemSettingDTO settings = systemSettingService.get();
        return settings == null ? null : settings.getUpload();
    }

    /**
     * 文件头（魔数）校验
     * <p>
     * 扩展名与 Content-Type 都来自客户端，可以被伪装成 jpg 的脚本绕过；
     * 真正的图片二进制头是伪造不了的，这里按声明扩展名校验文件头是否吻合
     */
    private void verifyMagicBytes(MultipartFile file, String extension) {
        byte[] header = readHeader(file, 12);
        if (header.length < 3) {
            throw new BusinessException("文件内容不是有效的图片");
        }
        boolean matches;
        switch (extension) {
            case "jpg":
                // JPEG：FF D8 FF
                matches = (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
                break;
            case "png":
                // PNG：89 50 4E 47 0D 0A 1A 0A
                matches = header.length >= 8 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50
                        && header[2] == 0x4E && header[3] == 0x47;
                break;
            case "gif":
                // GIF：'G''I''F' 开头（GIF87a / GIF89a）
                matches = header[0] == 'G' && header[1] == 'I' && header[2] == 'F';
                break;
            case "webp":
                // WebP：'RIFF' + 4 字节长度 + 'WEBP'
                matches = header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F'
                        && header[3] == 'F' && header[8] == 'W' && header[9] == 'E'
                        && header[10] == 'B' && header[11] == 'P';
                break;
            default:
                matches = false;
        }
        if (!matches) {
            log.warn("上传文件扩展名与文件头不匹配，疑似伪装图片，已拒绝，扩展名：{}", extension);
            throw new BusinessException("文件内容与图片格式不符，已拒绝上传");
        }
    }

    /**
     * 读取文件头部若干字节（Java 8 无 InputStream.readNBytes，手动循环读满）
     */
    private byte[] readHeader(MultipartFile file, int length) {
        byte[] buffer = new byte[length];
        int read = 0;
        try (InputStream in = file.getInputStream()) {
            while (read < length) {
                int n = in.read(buffer, read, length - read);
                if (n < 0) {
                    break;
                }
                read += n;
            }
        } catch (IOException e) {
            throw new BusinessException("无法读取上传文件内容");
        }
        return read == length ? buffer : Arrays.copyOf(buffer, read);
    }
}
