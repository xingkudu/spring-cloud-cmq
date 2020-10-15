package com.qcloud.cmq.core.config;

import com.qcloud.cmq.Account;
import com.qcloud.cmq.CMQClientInterceptor;
import com.qcloud.cmq.entity.CmqConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName CMQAccountAutoConfiguration
 * @Description cmq account
 * @Author hugo
 * @Date 2020/10/14 下午8:26
 * @Version 1.0
 **/
@Configuration
@AutoConfigureAfter(CMQAutoConfiguration.class)
public class CMQAccountAutoConfiguration {

    @Autowired(required = false)
    List<CMQClientInterceptor> interceptorList = new ArrayList<>();

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({CmqConfig.class})
    public Account account(CmqConfig cmqConfig) {
        return new Account(cmqConfig, interceptorList);
    }

}
