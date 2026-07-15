package com.sportsmate.server.infrastructure.aop;

import org.aspectj.lang.annotation.Pointcut;

public class Pointcuts {

    @Pointcut("execution(* com.sportsmate.server..*Controller.*(..))")
    public void allController() {
    }
}
