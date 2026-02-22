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
 * Python 只负责特征提取，Java 负责所有相似度比对。
 */
@Slf4j
@Service
public class PythonServiceClientImpl implements PythonServiceClient {

    @Autowired
    private PythonServiceProperties pythonServiceProperties;

    private final RestTemplate restTemplate = new RestTemplate();

    // ── /api/face/extract ───────────────────────────────────────────────────────

    @Override
    public String extractFaceFeature(String imageUrl) {
        String url = pythonServiceProperties.getBaseUrl() + "/api/face/extract";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> request = new HashMap<>();
            request.put("imageUrl", imageUrl);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(request, headers), String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());
                // Python 约定：code=1 成功，code=0 失败
                if (result.getInteger("code") == 1) {
                    // data 是 Python json.dumps(feature) 返回的 JSON 字符串，如 "[0.123, ...]"
                    return result.getString("data");
                }
                log.error("Python /extract 返回错误: {}", result.getString("msg"));
            }
        } catch (Exception e) {
            log.error("调用 Python /extract 失败: {}", e.getMessage());
        }
        return null;
    }

    // ── /api/face/detect ────────────────────────────────────────────────────────

    @Override
    public List<List<Double>> detectFaces(List<String> imageUrls) {
        String url = pythonServiceProperties.getBaseUrl() + "/api/face/detect";
        List<List<Double>> allEmbeddings = new ArrayList<>();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> request = new HashMap<>();
            request.put("imageUrls", imageUrls);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(request, headers), String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject result = JSON.parseObject(response.getBody());
                if (result.getInteger("code") != 1) {
                    log.error("Python /detect 返回错误: {}", result.getString("msg"));
                    return allEmbeddings;
                }

                // data = [ { imageIndex, faces: [ { bbox, embedding, detScore } ] } ]
                JSONArray dataArray = result.getJSONArray("data");
                for (int i = 0; i < dataArray.size(); i++) {
                    JSONObject imageResult = dataArray.getJSONObject(i);
                    JSONArray faces = imageResult.getJSONArray("faces");
                    for (int j = 0; j < faces.size(); j++) {
                        JSONObject face = faces.getJSONObject(j);
                        JSONArray embeddingArr = face.getJSONArray("embedding");
                        if (embeddingArr == null) continue;

                        List<Double> embedding = new ArrayList<>();
                        for (int k = 0; k < embeddingArr.size(); k++) {
                            embedding.add(embeddingArr.getDouble(k));
                        }
                        allEmbeddings.add(embedding);
                    }
                }
            }
        } catch (Exception e) {
            log.error("调用 Python /detect 失败: {}", e.getMessage());
        }

        return allEmbeddings;
    }

    // ── /health ─────────────────────────────────────────────────────────────────

    @Override
    public boolean healthCheck() {
        String url = pythonServiceProperties.getBaseUrl() + "/health";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Python 服务健康检查失败: {}", e.getMessage());
            return false;
        }
    }
}
