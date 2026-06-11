package com.cookmate.simulator.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    String authorization = request.getHeader("Authorization");
                    if (authorization != null && !template.headers().containsKey("Authorization")) {
                        template.header("Authorization", authorization);
                    }
                }
            }
        };
    }
    @Bean
    public feign.Request.Options feignOptions() {
        // 60 seconds connect timeout, 120 seconds read timeout
        return new feign.Request.Options(60000, TimeUnit.MILLISECONDS,
                                         120000, TimeUnit.MILLISECONDS,
                                         true);
    }

}
