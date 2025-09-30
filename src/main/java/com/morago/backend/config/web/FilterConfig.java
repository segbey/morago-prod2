package com.morago.backend.config.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<RequestSizeLimitFilter> requestSizeLimitFilter() {
        FilterRegistrationBean<RequestSizeLimitFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new RequestSizeLimitFilter());
        bean.setOrder(0);
        return bean;
    }
}