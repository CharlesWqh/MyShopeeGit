package com.shopee.shopeegit.seatalk;

import com.google.gson.Gson;
import com.shopee.shopeegit.gitlab.JsonHttpResponseCallback;
import com.shopee.shopeegit.gitlab.exception.AccessDeniedException;
import com.shopee.shopeegit.http.HttpClientFactory;
import okhttp3.*;
import org.bouncycastle.util.Strings;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SeaTalk {
    private final String webhookUrl;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();
    public SeaTalk(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.httpClient = HttpClientFactory.getInstance().getHttpClient();
    }

    public void callWebhook(String mergeUrl, String title, String mentionedList) {
        SeaTalkRequest seaTalkRequest = new SeaTalkRequest("text",
                new SeaTalkContent("Please pay attention! You have a new merge request pending" + "\n" + mergeUrl + "\n" + title,
                        convertToMentionedList(mentionedList)));
        CompletableFuture<SeaTalkResponse> result = new CompletableFuture<>();
        Request request = new Request.Builder()
                .url(webhookUrl)
                .post(RequestBody.create(MediaType.parse("application/json"), this.gson.toJson(seaTalkRequest)))
                .build();
        this.httpClient.newCall(request)
                .enqueue(new JsonHttpResponseCallback<>(SeaTalkResponse.class, result, this.gson) {
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
    }
    protected String getContentType(Response response) {
        List<String> headers = response.headers("Content-Type");
        if (headers.isEmpty()) {
            return "application/octet-stream";
        }
        return headers.get(0);
    }

    private String[] convertToMentionedList(String mentionedList) {
        return Strings.split(mentionedList, ',');
    }
}
