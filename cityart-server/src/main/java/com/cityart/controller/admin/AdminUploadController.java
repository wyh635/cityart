package com.cityart.controller.admin;

import com.cityart.result.Result;
import com.cityart.service.UploadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Map;

/**
 * B 端文件上传接口
 * <p>
 * 新增展览 / 新增展馆 / 编辑头像都先调这个接口拿到 URL，再提交表单
 */
@RestController
@RequestMapping("/api/admin")
@Api(tags = "B端文件上传接口")
@Slf4j
public class AdminUploadController {

    @Autowired
    private UploadService uploadService;

    /**
     * 上传图片
     * <p>
     * 限制：jpg / jpeg / png / webp，单张 ≤ 5MB。
     * 返回的是**相对 URL**（如 /static/upload/poster/20261015_a1b2c3d4.jpg），
     * 前端直接拼域名即可访问。
     *
     * @param file    图片文件
     * @param bizType 业务类型：1-展览海报 2-展馆海报 3-展馆简历 4-头像（不传归到 common 目录）
     */
    @PostMapping("/upload")
    @ApiOperation("上传图片")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "bizType", required = false) Integer bizType) {
        log.info("后台上传图片，bizType：{}，原始文件名：{}，大小：{} 字节",
                bizType, file.getOriginalFilename(), file.getSize());
        String url = uploadService.uploadImage(file, bizType);
        return Result.success("上传成功", Collections.singletonMap("url", url));
    }
}
