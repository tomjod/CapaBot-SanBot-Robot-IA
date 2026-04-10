package com.mundosvirtuales.visitorassistant.visitorflow;

public final class VisitorFlowEntryDecider {

    public enum EntryTarget {
        GUIDED_VISITOR_FLOW,
        LEGACY_DIALOG
    }

    private VisitorFlowEntryDecider() {
    }

    public static EntryTarget resolve(boolean visitorFlowEnabled, String apiBaseUrl) {
        return shouldUseGuidedFlow(visitorFlowEnabled, apiBaseUrl)
                ? EntryTarget.GUIDED_VISITOR_FLOW
                : EntryTarget.LEGACY_DIALOG;
    }

    static boolean shouldUseGuidedFlow(boolean visitorFlowEnabled, String apiBaseUrl) {
        return visitorFlowEnabled && apiBaseUrl != null && !apiBaseUrl.trim().isEmpty();
    }
}
