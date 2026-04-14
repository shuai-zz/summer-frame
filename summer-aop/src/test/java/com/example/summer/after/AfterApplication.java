package com.example.summer.after;

import org.example.annotation.Bean;
import org.example.annotation.Component;
import org.example.annotation.ComponentScan;
import org.example.annotation.Configuration;
import org.example.aop.AroundProxyBeanPostProcessor;

@Configuration
@ComponentScan
public class AfterApplication {
    @Bean
    AroundProxyBeanPostProcessor createAroundProxyBeanPostProcessor(){
        return new AroundProxyBeanPostProcessor();
    }
}
