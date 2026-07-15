package com.sportsmate.server.infrastructure.config;

import com.sportsmate.server.infrastructure.security.authorization.RoleAuthInterceptor;
import com.sportsmate.server.infrastructure.security.ratelimit.SmsIpRateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RoleAuthInterceptor roleAuthInterceptor;
    private final SmsIpRateLimitInterceptor smsIpRateLimitInterceptor;

    public WebConfig(RoleAuthInterceptor roleAuthInterceptor, SmsIpRateLimitInterceptor smsIpRateLimitInterceptor) {
        this.roleAuthInterceptor = roleAuthInterceptor;
        this.smsIpRateLimitInterceptor = smsIpRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(smsIpRateLimitInterceptor)
                .addPathPatterns(
                        "/api/v1/auth/signup/phone/send-code",
                        "/api/v1/auth/password/reset/send-code");
        registry.addInterceptor(roleAuthInterceptor);
    }
}
