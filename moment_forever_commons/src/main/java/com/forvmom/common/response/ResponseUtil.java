package com.forvmom.common.response;

import org.springframework.http.HttpStatus;

import java.util.List;

public class ResponseUtil {

    private ResponseUtil() {
        // Utility class - prevent instantiation
    }

    public static <T> ApiResponse<T> buildOkResponse(T data, String message) {
        return ApiResponse.<T>builder()
                .setCode(HttpStatus.OK.value())
                .setStatus("SUCCESS")
                .setMsg(message)
                .setResponse(data)
                .build();
    }

    public static <T> ApiResponse<T> buildCreatedResponse(T data, String message) {
        return ApiResponse.<T>builder()
                .setCode(HttpStatus.CREATED.value())
                .setStatus("SUCCESS")
                .setMsg(message)
                .setResponse(data)
                .build();
    }

    public static <T> ApiResponse<T> buildErrorResponse(String message, HttpStatus status) {
        return ApiResponse.<T>builder()
                .setCode(status.value())
                .setStatus("INTERNAL_SERVER_ERROR")
                .setMsg(message)
                .setResponse(null)
                .build();
    }

    public static <T> ApiResponse<T> buildErrorResponse(String message, HttpStatus status, List<String> errors) {
        return ApiResponse.<T>builder()
                .setCode(status.value())
                .setStatus("VALIDATION_ERROR")
                .setMsg(message)
                .setErrors(errors)
                .setResponse(null)
                .build();
    }

    public static <T> ApiResponse<T> buildConflictResponse(String message) {
        return ApiResponse.<T>builder()
                .setCode(HttpStatus.CONFLICT.value())
                .setStatus("CONFLICT")
                .setMsg(message)
                .setResponse(null)
                .build();
    }

    public static <T> ApiResponse<T> buildNotFoundResponse(String message) {
        return buildErrorResponse(message, HttpStatus.NOT_FOUND);
    }

    public static <T> ApiResponse<T> buildBadRequestResponse(String message) {
        return buildErrorResponse(message, HttpStatus.BAD_REQUEST);
    }

    public static <T> ApiResponse<T> buildValidationErrorResponse(String msg, List<String> errors) {
        return buildErrorResponse(msg, HttpStatus.BAD_REQUEST, errors);
    }
}