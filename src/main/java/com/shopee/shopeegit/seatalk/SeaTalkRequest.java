package com.shopee.shopeegit.seatalk;

public class SeaTalkRequest {

    public SeaTalkRequest(String tag, SeaTalkContent text) {
        this.tag = tag;
        this.text = text;
    }

    private String tag;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public SeaTalkContent getText() {
        return text;
    }

    public void setText(SeaTalkContent text) {
        this.text = text;
    }

    private SeaTalkContent text;
}
