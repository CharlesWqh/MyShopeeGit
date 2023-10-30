package com.shopee.shopeegit.jira;

import java.util.List;

public class JiraResponse {
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private String key;


    public JiraField getFields() {
        return fields;
    }

    public void setFields(JiraField fields) {
        this.fields = fields;
    }

    private JiraField fields;
}
