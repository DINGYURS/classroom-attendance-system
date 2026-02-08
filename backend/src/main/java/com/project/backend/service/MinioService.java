package com.project.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * MinIO 存储服务接口
 */
public interface MinioService {

    /**
     * 上传文件
     *
     * @param file     文件
     * @param folder   存储目录 (如 "faces", "attendance")
     * @return 文件访问 URL
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * 上传文件（指定文件名）
     *
     * @param file       文件
     * @param folder     存储目录
     * @param objectName 对象名称
     * @return 文件访问 URL
     */
    String uploadFile(MultipartFile file, String folder, String objectName);

    /**
     * 获取文件访问 URL
     *
     * @param objectKey 对象键
     * @return 文件访问 URL
     */
    String getFileUrl(String objectKey);

    /**
     * 删除文件
     *
     * @param objectKey 对象键
     */
    void deleteFile(String objectKey);
}
