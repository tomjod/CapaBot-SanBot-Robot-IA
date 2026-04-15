package com.mundosvirtuales.visitorassistant.features.visitor.domain;

public class VisitorInformationOption {

    private final String id;
    private final String title;
    private final int logoResId;
    private final String summary;
    private final String detail;

    public VisitorInformationOption(String id, String title, int logoResId, String summary, String detail) {
        this.id = id;
        this.title = title;
        this.logoResId = logoResId;
        this.summary = summary;
        this.detail = detail;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getLogoResId() {
        return logoResId;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetail() {
        return detail;
    }
}
