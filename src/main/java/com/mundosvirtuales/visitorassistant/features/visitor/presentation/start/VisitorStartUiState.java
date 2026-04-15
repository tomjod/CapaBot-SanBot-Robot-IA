package com.mundosvirtuales.visitorassistant.features.visitor.presentation.start;

public class VisitorStartUiState {

    public enum Screen {
        CHECKING,
        AVAILABLE,
        MAINTENANCE
    }

    private final Screen screen;
    private final String message;
    private final boolean receptionAccessEnabled;
    private final boolean informationAccessEnabled;
    private final boolean legacyAccessEnabled;
    private final boolean retryEnabled;

    private VisitorStartUiState(Screen screen,
                                String message,
                                boolean receptionAccessEnabled,
                                boolean informationAccessEnabled,
                                boolean legacyAccessEnabled,
                                boolean retryEnabled) {
        this.screen = screen;
        this.message = message;
        this.receptionAccessEnabled = receptionAccessEnabled;
        this.informationAccessEnabled = informationAccessEnabled;
        this.legacyAccessEnabled = legacyAccessEnabled;
        this.retryEnabled = retryEnabled;
    }

    public static VisitorStartUiState checking(String message) {
        return new VisitorStartUiState(Screen.CHECKING, message, false, true, true, false);
    }

    public static VisitorStartUiState available() {
        return new VisitorStartUiState(Screen.AVAILABLE, "", true, true, true, false);
    }

    public static VisitorStartUiState maintenance(String message) {
        return new VisitorStartUiState(Screen.MAINTENANCE, message, false, true, true, true);
    }

    public Screen getScreen() {
        return screen;
    }

    public String getMessage() {
        return message;
    }

    public boolean isReceptionAccessEnabled() {
        return receptionAccessEnabled;
    }

    public boolean isInformationAccessEnabled() {
        return informationAccessEnabled;
    }

    public boolean isLegacyAccessEnabled() {
        return legacyAccessEnabled;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }
}
