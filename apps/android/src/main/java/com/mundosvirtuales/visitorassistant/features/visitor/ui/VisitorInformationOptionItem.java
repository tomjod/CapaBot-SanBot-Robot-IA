package com.mundosvirtuales.visitorassistant.features.visitor.ui;

public class VisitorInformationOptionItem {

    private final String id;
    private final String title;
    private final int logoResId;
    private final String summary;

    public VisitorInformationOptionItem(String id, String title, int logoResId, String summary) {
        this.id = id;
        this.title = title;
        this.logoResId = logoResId;
        this.summary = summary;
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
}
