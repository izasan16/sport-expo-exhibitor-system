package com.sportexpo.exhibitor.common;

public record ApiResponse<T>(boolean success, T data, String message, String errorCode) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "OK", null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return new ApiResponse<>(false, null, message, errorCode);
    }
}
