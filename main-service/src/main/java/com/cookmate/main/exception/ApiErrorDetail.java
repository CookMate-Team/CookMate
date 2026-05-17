package com.cookmate.main.exception;

public record ApiErrorDetail(
    String field,
    String message
) {}
