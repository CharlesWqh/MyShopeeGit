package com.shopee.shopeegit.gitlab;

import java.util.List;

public class SeaTalkContent {
    public SeaTalkContent(String content) {
        this.content = content;
    }

    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getMentioned_list() {
        return mentioned_list;
    }

    public void setMentioned_list(List<String> mentioned_list) {
        this.mentioned_list = mentioned_list;
    }

    public List<String> getMentioned_email_list() {
        return mentioned_email_list;
    }

    public void setMentioned_email_list(List<String> mentioned_email_list) {
        this.mentioned_email_list = mentioned_email_list;
    }

    private List<String> mentioned_list;

    private List<String> mentioned_email_list;
}
