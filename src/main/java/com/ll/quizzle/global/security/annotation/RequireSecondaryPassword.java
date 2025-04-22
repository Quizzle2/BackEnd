package com.ll.quizzle.global.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD}) // 메서드에만 붙일 수 있는 어노테이션
@Retention(RetentionPolicy.RUNTIME) // 이 어노테이션이 언제까지 유지되는지
public @interface RequireSecondaryPassword {
	String value() default "";
}
