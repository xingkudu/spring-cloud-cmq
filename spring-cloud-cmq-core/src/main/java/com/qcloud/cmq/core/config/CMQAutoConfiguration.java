package com.qcloud.cmq.core.config;

import com.qcloud.cmq.entity.CmqConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @ClassName CMQAutoConfiguration
 * @Description cmq自动配置
 * @Author hugo
 * @Date 2020/10/14 上午11:32
 * @Version 1.0
 **/
@Configuration
@Import(CMQConfiguration.class)
public class CMQAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public CmqConfig cmqConfig(CMQConfiguration cmqConfiguration) {
        CmqConfig cmqConfig = new CmqConfig(cmqConfiguration.getEndpoint(), cmqConfiguration.getSecretId(), cmqConfiguration.getSecretKey(), cmqConfiguration.getPath(), cmqConfiguration.getMethod());
        cmqConfig.setSignMethod(cmqConfiguration.getSignMethod());
        return cmqConfig;
    }

}
