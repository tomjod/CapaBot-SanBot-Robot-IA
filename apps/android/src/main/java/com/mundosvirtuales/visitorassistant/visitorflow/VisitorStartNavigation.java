package com.mundosvirtuales.visitorassistant.visitorflow;

public final class VisitorStartNavigation {

    public enum VisitReason {
        TALK_TO_PERSON,
        LEAVE_MESSAGE,
        REQUEST_INFORMATION,
        LEGACY_BASE
    }

    public enum Target {
        START_MENU,
        CONTACT_FLOW,
        MESSAGE_FLOW,
        INFORMATION_FLOW,
        LEGACY_BASE
    }

    private VisitorStartNavigation() {
    }

    public static Target resolveAutomaticGreetingTarget() {
        return Target.START_MENU;
    }

    public static Target resolveTarget(VisitReason visitReason) {
        if (visitReason == VisitReason.TALK_TO_PERSON) {
            return Target.CONTACT_FLOW;
        }
        if (visitReason == VisitReason.LEAVE_MESSAGE) {
            return Target.MESSAGE_FLOW;
        }
        if (visitReason == VisitReason.REQUEST_INFORMATION) {
            return Target.INFORMATION_FLOW;
        }
        if (visitReason == VisitReason.LEGACY_BASE) {
            return Target.LEGACY_BASE;
        }
        return Target.INFORMATION_FLOW;
    }
}
