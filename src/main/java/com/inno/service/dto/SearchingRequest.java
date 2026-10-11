package com.inno.service.dto;

public class SearchingRequest {
    protected String value;

    public SearchingRequest() {
    }

    public SearchingRequest(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
