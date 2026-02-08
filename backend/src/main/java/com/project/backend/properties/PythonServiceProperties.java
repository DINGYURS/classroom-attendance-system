package com.project.backend.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Python 算法服务配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "rollcallsystem.python-service")
public class PythonServiceProperties {

    /**
     * Python 算法服务基础 URL
     */
    private String baseUrl;
}
