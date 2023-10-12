package com.shopee.shopeegit.gitlab.exception;

public class DuplicateMergeRequestException extends RuntimeException {

    public DuplicateMergeRequestException() {
        super();
    }

    public DuplicateMergeRequestException(String msg) {
        super(msg);
    }

    public DuplicateMergeRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public DuplicateMergeRequestException(Throwable cause) {
        super(cause);
    }
}