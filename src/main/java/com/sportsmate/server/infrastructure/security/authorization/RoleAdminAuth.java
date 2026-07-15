package com.sportsmate.server.infrastructure.security.authorization;

import com.sportsmate.server.common.enums.Role;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@RoleAuth(role = Role.ADMIN)
public @interface RoleAdminAuth {
}
