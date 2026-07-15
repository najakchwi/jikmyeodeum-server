package com.sportsmate.server.infrastructure.aop;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Controller API 호출을 AOP로 로깅한다.
 *
 * <p>성공 로그 예시:
 * [API] POST /api/v1/users -&gt; 201 | UserController.register() | params={} | time=32ms
 *
 * <p>예외 로그 예시:
 * [API ERROR] GET /api/v1/users/abc | UserController.getUser() | params={} | time=5ms | message=...
 */
@Slf4j
@Aspect
@Order(1)
@Component
@Profile("!test")
public class ApiLoggingAspect {

    @Around("com.sportsmate.server.infrastructure.aop.Pointcuts.allController()")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (request != null) {
                log.info(
                        "[API] {} {} -> {} | {}.{}() | params={} | time={}ms",
                        request.getMethod(),
                        decodedRequestUri(request),
                        statusCodeValue(result),
                        joinPoint.getSignature().getDeclaringType().getSimpleName(),
                        joinPoint.getSignature().getName(),
                        params(request),
                        elapsedTime);
            }

            return result;
        } catch (Exception ex) {
            long elapsedTime = System.currentTimeMillis() - startTime;

            if (request != null) {
                log.error(
                        "[API ERROR] {} {} | {}.{}() | params={} | time={}ms | message={}",
                        request.getMethod(),
                        decodedRequestUri(request),
                        joinPoint.getSignature().getDeclaringType().getSimpleName(),
                        joinPoint.getSignature().getName(),
                        params(request),
                        elapsedTime,
                        ex.getMessage(),
                        ex);
            }

            throw ex;
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String decodedRequestUri(HttpServletRequest request) {
        try {
            return URLDecoder.decode(request.getRequestURI(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return request.getRequestURI();
        }
    }

    private Map<String, Object> params(HttpServletRequest request) {
        Map<String, Object> params = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            String normalizedKey = key.replace(".", "-");
            if (values == null || values.length == 0) {
                params.put(normalizedKey, "");
            } else if (values.length == 1) {
                params.put(normalizedKey, values[0]);
            } else {
                params.put(normalizedKey, values);
            }
        });
        return params;
    }

    private int statusCodeValue(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }
        return 200;
    }
}
