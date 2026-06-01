package com.mundosvirtuales.visitorassistant.features.visitor.domain;

public class VisitorEntryRequest {

    private final boolean busy;
    private final boolean botReady;
    private final String visitorName;
    private final boolean visitorFlowEnabled;
    private final String apiBaseUrl;
    private final int hourOfDay;
    private final double daypartGreetingChance;

    public VisitorEntryRequest(boolean busy,
                               boolean botReady,
                               String visitorName,
                               boolean visitorFlowEnabled,
                               String apiBaseUrl,
                               int hourOfDay,
                               double daypartGreetingChance) {
        this.busy = busy;
        this.botReady = botReady;
        this.visitorName = visitorName;
        this.visitorFlowEnabled = visitorFlowEnabled;
        this.apiBaseUrl = apiBaseUrl;
        this.hourOfDay = hourOfDay;
        this.daypartGreetingChance = daypartGreetingChance;
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean isBotReady() {
        return botReady;
    }

    public String getVisitorName() {
        return visitorName;
    }

    public boolean isVisitorFlowEnabled() {
        return visitorFlowEnabled;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public int getHourOfDay() {
        return hourOfDay;
    }

    public double getDaypartGreetingChance() {
        return daypartGreetingChance;
    }
}
