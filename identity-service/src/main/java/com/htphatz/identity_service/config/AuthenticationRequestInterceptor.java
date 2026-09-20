package com.htphatz.identity_service.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class AuthenticationRequestInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (servletRequestAttributes != null && servletRequestAttributes.getRequest() != null) {
            String authHeader = servletRequestAttributes.getRequest().getHeader("Authorization");
            if (StringUtils.hasText(authHeader)) {
                log.info("Forwarding Authorization header to Feign client");
                requestTemplate.header("Authorization", authHeader);
            }
        }
    }
}
