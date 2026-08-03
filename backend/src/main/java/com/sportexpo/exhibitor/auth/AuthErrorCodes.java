package com.sportexpo.exhibitor.auth;

public final class AuthErrorCodes {
    public static final String BAD_CREDENTIALS = "E_AUTH_BAD_CREDENTIALS";
    public static final String ACCOUNT_LOCKED = "E_AUTH_ACCOUNT_LOCKED";
    public static final String USER_DISABLED = "E_AUTH_USER_DISABLED";
    public static final String TOKEN_EXPIRED = "E_AUTH_TOKEN_EXPIRED";
    public static final String TOKEN_INVALID = "E_AUTH_TOKEN_INVALID";

    private AuthErrorCodes() {
    }
}
