package com.sleekydz86.catalog.global.config.openapi;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Parameter(
        name = "userId",
        in = ParameterIn.HEADER,
        description = "작업 수행자 ID. 미입력 시 `system`으로 기록됩니다.",
        example = "etl-operator",
        required = false
)
public @interface UserIdHeader {
}
