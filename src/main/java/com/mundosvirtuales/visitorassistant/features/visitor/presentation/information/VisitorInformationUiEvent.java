package com.mundosvirtuales.visitorassistant.features.visitor.presentation.information;

public class VisitorInformationUiEvent {

    private final String speechText;

    public VisitorInformationUiEvent(String speechText) {
        this.speechText = speechText;
    }

    public String getSpeechText() {
        return speechText;
    }
}
