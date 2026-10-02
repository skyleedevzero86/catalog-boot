package com.sleekydz86.catalog.global.exception;

public class InfrastructureException extends BusinessException {

    public InfrastructureException(ErrorCode code, String message) {
        super(code == null ? ErrorCode.INTERNAL_SERVER_ERROR : code, message);
    }

    public InfrastructureException(ErrorCode code, String message, Throwable cause) {
        super(code == null ? ErrorCode.INTERNAL_SERVER_ERROR : code, message, cause);
    }

    public static InfrastructureException of(ErrorCode code, String message, Throwable cause) {
        if (cause == null) {
            return new InfrastructureException(code, message);
        }
        return new InfrastructureException(code, message, cause);
    }
}
