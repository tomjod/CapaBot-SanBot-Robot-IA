package com.mundosvirtuales.visitorassistant.features.visitor.presentation.start;

public class VisitorStartUiEvent {

    public enum Target {
        CONTACT_FLOW,
        MESSAGE_FLOW,
        INFORMATION_FLOW,
        LEGACY_BASE
    }

    private final Target target;
    private final String visitorName;

    public VisitorStartUiEvent(Target target, String visitorName) {
        this.target = target;
        this.visitorName = visitorName;
    }

    public Target getTarget() {
        return target;
    }

    public String getVisitorName() {
        return visitorName;
    }
}
