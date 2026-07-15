package com.sportsmate.server.infrastructure.security.authorization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sportsmate.server.common.enums.Role;
import com.sportsmate.server.common.exception.BusinessException;
import com.sportsmate.server.common.exception.CommonErrorCode;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

@DisplayName("RoleAuthInterceptor 단위 테스트")
class RoleAuthInterceptorTest {

    private final RoleAuthInterceptor interceptor = new RoleAuthInterceptor();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("관리자 권한이 필요한 핸들러에 미인증으로 접근하면 401 예외가 발생한다")
    void preHandle_adminHandlerWithoutAuthentication_throwsUnauthorized() throws NoSuchMethodException {
        HandlerMethod handlerMethod = adminHandler("adminEndpoint");

        assertThatThrownBy(() -> interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("관리자 권한이 필요한 핸들러에 USER 권한으로 접근하면 403 예외가 발생한다")
    void preHandle_adminHandlerWithUserRole_throwsForbidden() throws NoSuchMethodException {
        authenticate("ROLE_USER");
        HandlerMethod handlerMethod = adminHandler("adminEndpoint");

        assertThatThrownBy(() -> interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    @Test
    @DisplayName("관리자 권한이 필요한 핸들러에 ADMIN 권한으로 접근하면 통과한다")
    void preHandle_adminHandlerWithAdminRole_returnsTrue() throws Exception {
        authenticate("ROLE_ADMIN");
        HandlerMethod handlerMethod = adminHandler("adminEndpoint");

        boolean result = interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("역할 어노테이션이 없는 핸들러는 인증 없이 통과한다")
    void preHandle_handlerWithoutRoleAuth_returnsTrue() throws Exception {
        HandlerMethod handlerMethod = publicHandler("publicEndpoint");

        boolean result = interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("클래스 레벨이 ADMIN을 요구해도 메서드 레벨 어노테이션이 우선 적용된다")
    void preHandle_methodLevelRoleAuth_overridesClassLevel() throws Exception {
        authenticate("ROLE_USER");
        HandlerMethod handlerMethod = mixedHandler("methodLevelOverridesClassLevel");

        boolean result = interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod);

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("메서드 레벨 어노테이션이 없으면 클래스 레벨(ADMIN) 요구가 그대로 적용된다")
    void preHandle_methodWithoutOwnRoleAuth_fallsBackToClassLevel() throws Exception {
        authenticate("ROLE_USER");
        HandlerMethod handlerMethod = mixedHandler("inheritsClassLevel");

        assertThatThrownBy(() -> interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handlerMethod))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.FORBIDDEN));
    }

    private void authenticate(String authority) {
        var authentication = new UsernamePasswordAuthenticationToken(
                "member-1",
                null,
                List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private HandlerMethod adminHandler(String methodName) throws NoSuchMethodException {
        Method method = AdminController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new AdminController(), method);
    }

    private HandlerMethod publicHandler(String methodName) throws NoSuchMethodException {
        Method method = PublicController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new PublicController(), method);
    }

    private HandlerMethod mixedHandler(String methodName) throws NoSuchMethodException {
        Method method = MixedController.class.getDeclaredMethod(methodName);
        return new HandlerMethod(new MixedController(), method);
    }

    @RoleAdminAuth
    private static class AdminController {
        void adminEndpoint() {
        }
    }

    private static class PublicController {
        void publicEndpoint() {
        }
    }

    @RoleAdminAuth
    private static class MixedController {
        @RoleAuth(role = Role.USER)
        void methodLevelOverridesClassLevel() {
        }

        void inheritsClassLevel() {
        }
    }
}
