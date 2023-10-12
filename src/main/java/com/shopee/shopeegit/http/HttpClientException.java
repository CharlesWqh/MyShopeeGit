package com.shopee.shopeegit.http;

public class HttpClientException extends RuntimeException {

    public HttpClientException() {
        super();
    }

    public HttpClientException(String msg) {
        super(msg);
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpClientException(Throwable cause) {
        super(cause);
    }
}