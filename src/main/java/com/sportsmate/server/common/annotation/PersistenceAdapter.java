package com.sportsmate.server.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * 영속성 처리를 담당하는 어댑터임을 명시한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface PersistenceAdapter {

    @AliasFor(annotation = Component.class)
    String value() default "";
}
