package com.sportsmate.server.infrastructure.security.authorization;

import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RoleAuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RoleAuth roleAuth = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getMethod(), RoleAuth.class);
        if (roleAuth == null) {
            roleAuth = AnnotatedElementUtils.findMergedAnnotation(handlerMethod.getBeanType(), RoleAuth.class);
        }
        if (roleAuth == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
        }

        Set<String> requiredRoles = Arrays.stream(roleAuth.role())
                .map(role -> "ROLE_" + role.name())
                .collect(Collectors.toSet());
        boolean hasRequiredRole = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(requiredRoles::contains);
        if (!hasRequiredRole) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN);
        }
        return true;
    }
}
