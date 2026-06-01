package com.mundosvirtuales.visitorassistant.features.visitor.domain;

public class VisitorEntryScript {

    private final String hiMessage;
    private final String genericFlowIntro;
    private final String namedFlowIntroTemplate;
    private final String earlyMorningMessage;
    private final String morningMessage;
    private final String afternoonMessage;
    private final String nightMessage;

    public VisitorEntryScript(String hiMessage,
                              String genericFlowIntro,
                              String namedFlowIntroTemplate,
                              String earlyMorningMessage,
                              String morningMessage,
                              String afternoonMessage,
                              String nightMessage) {
        this.hiMessage = hiMessage;
        this.genericFlowIntro = genericFlowIntro;
        this.namedFlowIntroTemplate = namedFlowIntroTemplate;
        this.earlyMorningMessage = earlyMorningMessage;
        this.morningMessage = morningMessage;
        this.afternoonMessage = afternoonMessage;
        this.nightMessage = nightMessage;
    }

    public String getHiMessage() {
        return hiMessage;
    }

    public String getGenericFlowIntro() {
        return genericFlowIntro;
    }

    public String getNamedFlowIntroTemplate() {
        return namedFlowIntroTemplate;
    }

    public String getEarlyMorningMessage() {
        return earlyMorningMessage;
    }

    public String getMorningMessage() {
        return morningMessage;
    }

    public String getAfternoonMessage() {
        return afternoonMessage;
    }

    public String getNightMessage() {
        return nightMessage;
    }
}
