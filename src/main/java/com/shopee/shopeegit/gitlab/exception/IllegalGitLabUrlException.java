package com.shopee.shopeegit.gitlab.exception;

public class IllegalGitLabUrlException extends RuntimeException {

    public IllegalGitLabUrlException() {
        super();
    }

    public IllegalGitLabUrlException(String msg) {
        super(msg);
    }

    public IllegalGitLabUrlException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalGitLabUrlException(Throwable cause) {
        super(cause);
    }
}