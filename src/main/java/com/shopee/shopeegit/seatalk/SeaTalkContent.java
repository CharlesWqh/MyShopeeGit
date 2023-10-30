package com.shopee.shopeegit.seatalk;

import java.util.List;

public class SeaTalkContent {
    public SeaTalkContent(String content, String[] mentioned_email_list) {
        this.content = content;
        this.mentioned_email_list = mentioned_email_list;
    }

    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String[] getMentioned_list() {
        return mentioned_list;
    }

    public void setMentioned_list(String[] mentioned_list) {
        this.mentioned_list = mentioned_list;
    }

    public String[] getMentioned_email_list() {
        return mentioned_email_list;
    }

    public void setMentioned_email_list(String[] mentioned_email_list) {
        this.mentioned_email_list = mentioned_email_list;
    }

    private String[] mentioned_list;

    private String[] mentioned_email_list;
}
