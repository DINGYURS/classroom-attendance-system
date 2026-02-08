package com.project.backend.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.project.backend.properties.PythonServiceProperties;
import com.project.backend.service.PythonServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Python 算法服务客户端实现类
 */
@Slf4j
@Service
public class PythonServiceClientImpl implements PythonServiceClient {

    @Autowired
    private PythonServiceProperties pythonServiceProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String extractFaceFeature(String imageUrl) {
        String url = pythonServiceProperties.getBaseUrl() + "/api/face/extract";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = new HashMap<>();
            request.put("imageUrl", imageUrl);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());
                if (result.getInteger("code") == 0) {
                    return result.getString("data");
                } else {
                    log.error("Python 服务返回错误: {}", result.getString("msg"));
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("调用 Python 人脸特征提取服务失败: {}", e.getMessage());
        }

        return null;
    }

    @Override
    public List<Map<String, Object>> recognizeFaces(List<String> imageUrls, Map<Long, String> studentFeatures) {
        String url = pythonServiceProperties.getBaseUrl() + "/api/face/recognize";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("imageUrls", imageUrls);

            // 转换学生特征为 Python 服务期望的格式
            List<Map<String, Object>> features = new ArrayList<>();
            for (Map.Entry<Long, String> entry : studentFeatures.entrySet()) {
                Map<String, Object> feature = new HashMap<>();
                feature.put("studentId", entry.getKey());
                feature.put("featureVector", entry.getValue());
                features.add(feature);
            }
            request.put("studentFeatures", features);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());
                if (result.getInteger("code") == 0) {
                    JSONArray data = result.getJSONArray("data");
                    List<Map<String, Object>> results = new ArrayList<>();
                    for (int i = 0; i < data.size(); i++) {
                        JSONObject item = data.getJSONObject(i);
                        Map<String, Object> map = new HashMap<>();
                        map.put("studentId", item.getLong("studentId"));
                        map.put("similarity", item.getDouble("similarity"));
                        map.put("matched", item.getBoolean("matched"));
                        results.add(map);
                    }
                    return results;
                } else {
                    log.error("Python 服务返回错误: {}", result.getString("msg"));
                }
            }
        } catch (Exception e) {
            log.error("调用 Python 人脸识别服务失败: {}", e.getMessage());
        }

        return new ArrayList<>();
    }

    @Override
    public boolean healthCheck() {
        String url = pythonServiceProperties.getBaseUrl() + "/health";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Python 服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
