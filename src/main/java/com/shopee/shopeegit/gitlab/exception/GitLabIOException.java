package com.shopee.shopeegit.gitlab.exception;

public class GitLabIOException extends RuntimeException {
    public GitLabIOException(String message, Throwable cause) {
        super(message, cause);
    }
}