package com.qcloud.cmq.sleuth.config;

import brave.Tracing;
import com.qcloud.cmq.core.config.CMQAccountAutoConfiguration;
import com.qcloud.cmq.core.config.CMQAutoConfiguration;
import com.qcloud.cmq.entity.CmqConfig;
import com.qcloud.cmq.sleuth.instrument.TraceCMQClientInterceptor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName CMQTraceAutoConfiguration
 * @Description cmq trace 自动配置
 * @Author hugo
 * @Date 2020/10/14 下午8:05
 * @Version 1.0
 **/
@Configuration
@ConditionalOnBean({Tracing.class})
@AutoConfigureAfter(name = {"org.springframework.cloud.sleuth.autoconfig.TraceAutoConfiguration"}, value = {CMQAutoConfiguration.class})
@AutoConfigureBefore({CMQAccountAutoConfiguration.class})
public class CMQTraceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({Tracing.class, CmqConfig.class})
    public TraceCMQClientInterceptor traceCMQClientInterceptor(Tracing tracing, CmqConfig cmqConfig){
        return new TraceCMQClientInterceptor(tracing, cmqConfig);
    }

}
