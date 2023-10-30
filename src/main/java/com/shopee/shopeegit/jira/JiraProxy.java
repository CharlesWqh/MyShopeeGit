package com.shopee.shopeegit.jira;

import com.google.gson.Gson;
import com.shopee.shopeegit.http.HttpClientFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

public class JiraProxy {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static String url = "https://jira.shopee.io/rest/api/latest/issue/";

    private String privateToken;
    private final OkHttpClient httpClient;
    private final Gson gson = new Gson();

    public JiraProxy(String username, String pwd) {
        this.privateToken = "Basic " + base64Encode(username + ":" + pwd);
        this.httpClient = HttpClientFactory.getInstance().getHttpClient();
    }

    private String base64Encode(String str) {
        byte[] data = str.getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(data);
    }

    public String getSummaryByIssueNo(String issueNo) {
        String queryUrl = url + issueNo +"?fields=summary";
        Request request = new Request.Builder()
                .url(queryUrl)
                .addHeader(AUTHORIZATION_HEADER, this.privateToken)
                .get().build();
        try {
            Response response = this.httpClient.newCall(request).execute();
            ResponseBody body = response.body();
            if (body == null) {
                return "";
            }
            String json = body.string();
            JiraResponse jiraResponse = gson.fromJson(json, JiraResponse.class);
            if (jiraResponse == null || jiraResponse.getFields() == null) {
                return "";
            }
            return jiraResponse.getFields().getSummary();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
