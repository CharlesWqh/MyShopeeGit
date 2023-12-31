package com.shopee.shopeegit.gitlab;

import com.shopee.shopeegit.gitlab.exception.*;
import com.shopee.shopeegit.http.HttpClientFactory;
import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class GitLab {
    public static final String PRIVATE_TOKEN_HEADER = "Private-Token";

    private String baseUri;

    private String privateToken;

    private OkHttpClient httpClient;

    private Gson gson = new Gson();

    public GitLab(String baseUri, String privateToken) {
        this(baseUri, privateToken, false);
    }

    public GitLab(String baseUri, String privateToken, boolean allowSelfSignedTls) {
        this.baseUri = baseUri;
        this.privateToken = privateToken;

        HttpClientFactory httpClientFactory = HttpClientFactory.getInstance();
        this.httpClient = allowSelfSignedTls ? httpClientFactory.getInsecureHttpClient() : httpClientFactory.getHttpClient();
    }

    public CompletableFuture<VersionResponse> version() {
        String url = this.baseUri + "/version";

        CompletableFuture<VersionResponse> result = new CompletableFuture<>();
        if (HttpUrl.parse(url) == null) {
            result.completeExceptionally(new HttpResponseException(500, "Incorrect GitLab URL"));
            return result;
        }
        if (this.baseUri.endsWith("/")) {
            result.completeExceptionally(new HttpResponseException(500, "Remove last slash from URL"));
            return result;
        }

        Request request = prepareRequest("/version").build();
        this.httpClient.newCall(request)
                .enqueue(new JsonHttpResponseCallback<>(VersionResponse.class, result, this.gson) {
                    @Override
                    protected void onRawResponseBody(Response response, String rawResponseBodyString) {
                        if (response.code() != 200) {
                            String contentType = getContentType(response);
                            result.completeExceptionally(new AccessDeniedException.GitLabHttpResponseException(response.code(), response.message(), rawResponseBodyString, contentType));
                        } else {
                            super.onRawResponseBody(response, rawResponseBodyString);
                        }
                    }

                });
        return result;
    }

    public CompletableFuture<MergeRequestResponse> createMergeRequest(String gitLabProjectId, MergeRequestRequest mergeRequestRequest) {
        return doCreateMergeRequest(ProjectId.of(gitLabProjectId), mergeRequestRequest);
    }

    protected CompletableFuture<MergeRequestResponse> doCreateMergeRequest(ProjectId projectId, MergeRequestRequest mergeRequestRequest) {
        String url = this.baseUri + "/projects/" + projectId.getUrlEncoded() + "/merge_requests";

        Request request = new Request.Builder()
                .url(url)
                .addHeader(PRIVATE_TOKEN_HEADER, this.privateToken)
                .post(RequestBody.create(MediaType.parse("application/json"), this.gson.toJson(mergeRequestRequest)))
                .build();

        CompletableFuture<MergeRequestResponse> result = new CompletableFuture<>();

        Call call = httpClient.newCall(request);
        call.enqueue(new JsonHttpResponseCallback<>(MergeRequestResponse.class, result, this.gson) {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.code() == 409) {
                    completeExceptionally(result, new DuplicateMergeRequestException(), response);
                    return;
                }
                if (response.code() == 400) {
                    completeExceptionally(result, new BadMergeRequestException(response.message()), response);
                    return;
                }
                if (response.code() == 403 || response.code() == 401) {
                    completeExceptionally(result, new AccessDeniedException(), response);
                    return;
                }

                if (!isJson(response)) {
                    completeExceptionally(result, new UnsupportedContentTypeException("GitLab API is misconfigured. Only JSON replies are supported"), response);
                    return;
                }
                try (ResponseBody body = response.body()) {
                    if (body == null) {
                        result.completeExceptionally(new AccessDeniedException.GitLabHttpResponseException(response.code(), response.message(), null, null));
                        return;
                    }
                    String json = body.string();
                    logRawResponseBody(response, json);
                    if (isGitLabProjectNotFound(response, json)) {
                        result.completeExceptionally(GitLabProjectNotFoundException.of(projectId));
                        return;
                    }
                    MergeRequestResponse mergeRequestResponse = gson.fromJson(json, MergeRequestResponse.class);
                    result.complete(mergeRequestResponse);
                }
            }
        });
        return result;
    }

    @Nullable
    private String getContentType(@Nullable ResponseBody body) {
        if (body == null) {
            return null;
        }
        MediaType mediaType = body.contentType();
        if (mediaType == null) {
            return null;
        }
        return mediaType.toString();
    }

    private boolean isGitLabProjectNotFound(Response response, String json) {
        // {"message":"404 Project Not Found"}
        return 404 == response.code() && json.contains("Project Not Found");
    }

    private boolean isJson(Response response) {
        return Optional.ofNullable(response.header("Content-Type"))
                       .map(MediaType::parse)
                       .filter(contentType -> "application".equals(contentType.type()) && "json".equals(contentType.subtype()))
                       .isPresent();
    }

    protected Request.Builder prepareRequest(String urlSuffix) {
        return new Request.Builder()
                .url(this.baseUri + urlSuffix)
                .addHeader(PRIVATE_TOKEN_HEADER, this.privateToken);
    }

    public static String getBaseUrl(String gitLabRestApiUrl) {
        String url = gitLabRestApiUrl;
        url = StringUtils.removeEnd(url, "/");
        url = StringUtils.removeEnd(url, "/api/v4");
        return url;
    }

    protected String getContentType(Response response) {
        List<String> headers = response.headers("Content-Type");
        if (headers.isEmpty()) {
            return "application/octet-stream";
        }
        return headers.get(0);
    }

    public static String getAccessTokenWebPageUrl(String gitLabBaseUrl) {
        return gitLabBaseUrl + "/profile/personal_access_tokens";
    }
}
