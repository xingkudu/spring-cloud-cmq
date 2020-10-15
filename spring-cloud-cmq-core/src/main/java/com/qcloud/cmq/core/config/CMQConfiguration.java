package com.qcloud.cmq.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName CMQConfiguration
 * @Description cmq configration集成spring配置
 * @Author hugo
 * @Date 2020/10/14 上午11:22
 * @Version 1.0
 **/
@Configuration
@ConfigurationProperties(
        prefix = "cmq.server"
)
public class CMQConfiguration {

    private String endpoint;
    private String path = "/v2/index.php";
    private String secretId;
    private String secretKey;
    private String method = "POST";
    private String signMethod = "sha1";

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSecretId() {
        return secretId;
    }

    public void setSecretId(String secretId) {
        this.secretId = secretId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getSignMethod() {
        return signMethod;
    }

    public void setSignMethod(String signMethod) {
        this.signMethod = signMethod;
    }
}
