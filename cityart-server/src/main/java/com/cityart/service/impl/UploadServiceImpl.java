package com.cityart.service.impl;

import com.cityart.constant.MessageConstant;
import com.cityart.exception.BaseException;
import com.cityart.properties.UploadProperties;
import com.cityart.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 文件上传实现（本地磁盘存储）
 * <p>
 * 存储结构：{basePath}/{子目录}/{yyyyMMdd}_{随机8位}.{扩展名}
 * 例：./uploads/poster/20261015_a1b2c3d4.jpg
 * <p>
 * 访问方式：{@code /static/upload/**} 由 WebMvcConfiguration 映射到 {basePath}/
 * <p>
 * ⚠️ 生产环境应换成 OSS / 对象存储，本实现适合开发与单机部署。
 */
@Service
@Slf4j
public class UploadServiceImpl implements UploadService {

    /** 允许的图片扩展名 */
    private static final List<String> ALLOWED_EXT = Arrays.asList("jpg", "jpeg", "png", "webp");

    /** 单文件大小上限：5MB */
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    /** bizType → 子目录名 */
    private static final Map<Integer, String> BIZ_DIR = new LinkedHashMap<>();

    static {
        BIZ_DIR.put(1, "poster");        // 展览海报
        BIZ_DIR.put(2, "gallery");       // 展馆海报
        BIZ_DIR.put(3, "gallery-intro"); // 展馆简历
        BIZ_DIR.put(4, "avatar");        // 头像
    }

    private static final String DEFAULT_DIR = "common";

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private UploadProperties uploadProperties;

    /**
     * 启动时确保根目录存在，避免第一次上传才报错
     * <p>
     * 这里把配置里的相对路径解析成**绝对路径**并记日志，
     * 便于排查「上传成功但访问 404」这类问题（多半是 WebMvcConfiguration 与这里解析出的目录不一致）。
     */
    @PostConstruct
    public void initBaseDir() {
        File base = resolveBaseDir();
        if (!base.exists() && !base.mkdirs()) {
            log.error("【文件上传】无法创建上传根目录：{}，请检查磁盘权限", base.getPath());
        } else {
            log.info("【文件上传】上传根目录（绝对路径）：{}", base.getPath());
        }
    }

    /**
     * 把配置的 basePath 解析为绝对路径
     * <p>
     * 相对路径的基准是**进程工作目录**，所以这里统一 normalize 一下，
     * 保证 UploadServiceImpl 与 WebMvcConfiguration 拿到的是同一个目录。
     */
    static File resolveBaseDir(String basePath) {
        return new File(basePath).getAbsoluteFile();
    }

    private File resolveBaseDir() {
        return resolveBaseDir(uploadProperties.getBasePath());
    }

    @Override
    public String uploadImage(MultipartFile file, Integer bizType) {
        // 1. 基本校验
        if (file == null || file.isEmpty()) {
            throw new BaseException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BaseException("图片不能超过 5MB");
        }

        // 2. 校验扩展名
        String ext = extractExt(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext)) {
            throw new BaseException("只支持 jpg / jpeg / png / webp 格式的图片");
        }

        // 3. 校验「真实文件类型」——必须读文件头魔数
        //    ⚠️ 千万不要只信 file.getContentType()：它来自客户端声明的 Content-Type，可以随便伪造；
        //       实战中把 evil.txt 改名成 evil.png、并把 Content-Type 写成 image/png 就能绕过。
        if (!isRealImage(file, ext)) {
            throw new BaseException("文件内容不是有效图片，请上传真实的 jpg / png / webp 图片");
        }

        // 4. 组装落盘路径
        String subDir = BIZ_DIR.getOrDefault(bizType, DEFAULT_DIR);
        String fileName = LocalDate.now().format(DATE_FMT) + "_"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8) + "." + ext;

        File dir = new File(resolveBaseDir(), subDir);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }
        File dest = new File(dir, fileName);

        // 5. 写文件
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (IOException e) {
            log.error("【文件上传】写入失败，目标：{}", dest.getAbsolutePath(), e);
            throw new BaseException(MessageConstant.UPLOAD_FAILED);
        }

        String url = uploadProperties.getUrlPrefix() + "/" + subDir + "/" + fileName;
        log.info("【文件上传】成功，bizType：{}，大小：{} 字节，URL：{}", bizType, file.getSize(), url);
        return url;
    }

    /** 取小写扩展名；没有扩展名时返回空串（会被白名单挡掉） */
    private String extractExt(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int idx = originalFilename.lastIndexOf('.');
        if (idx < 0 || idx == originalFilename.length() - 1) {
            return "";
        }
        return originalFilename.substring(idx + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 通过**文件头魔数**判断是不是真图片
     * <p>
     * 各格式的文件头（前几个字节是固定的，改后缀改不掉）：
     * <ul>
     *     <li>JPEG：{@code FF D8 FF}</li>
     *     <li>PNG：{@code 89 50 4E 47 0D 0A 1A 0A}</li>
     *     <li>WebP：{@code 52 49 46 46 ...}（RIFF）开头，第 8-11 字节是 {@code 57 45 42 50}（WEBP）</li>
     * </ul>
     * 只读取开头几个字节，不会把整个文件读进内存。
     */
    private boolean isRealImage(MultipartFile file, String ext) {
        byte[] head = new byte[12];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(head);
            if (read < 4) {
                return false;
            }
        } catch (IOException e) {
            log.error("【文件上传】读取文件头失败", e);
            return false;
        }

        switch (ext) {
            case "jpg":
            case "jpeg":
                return (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF;
            case "png":
                return (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
            case "webp":
                // RIFF....WEBP
                return head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                        && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
            default:
                return false;
        }
    }
}
