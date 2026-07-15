package com.sportsmate.server.infrastructure.adapter.in.web.common.swagger.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodeExample {

    Class<? extends Enum<?>> codeType() default NoErrorCode.class;

    String code() default "";

    int status() default 0;

    String message() default "";

    String exampleName() default "";
}
