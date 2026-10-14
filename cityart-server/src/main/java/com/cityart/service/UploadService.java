package com.cityart.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传业务
 */
public interface UploadService {

    /**
     * 上传图片
     *
     * @param file    图片文件（jpg / jpeg / png / webp，≤5MB）
     * @param bizType 业务类型：1-展览海报 2-展馆海报 3-展馆简历 4-头像
     * @return 可直接给前端展示的相对 URL，如 /static/upload/poster/20261015_a1b2c3.jpg
     */
    String uploadImage(MultipartFile file, Integer bizType);
}
