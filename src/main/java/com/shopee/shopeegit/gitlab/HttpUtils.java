package com.shopee.shopeegit.gitlab;

import com.shopee.shopeegit.gitlab.exception.AccessDeniedException;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

public abstract class HttpUtils {
    @NotNull
    public static ResponseBody assertHasBody(Response response, ResponseBody body) {
        if (body == null) {
            throw AccessDeniedException.GitLabHttpResponseException.ofNullResponse(response);
        }
        return body;
    }
}
