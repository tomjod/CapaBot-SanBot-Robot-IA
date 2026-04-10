package com.mundosvirtuales.visitorassistant.visitorflow;

public class VisitorLauncherState {

    public enum Screen {
        CHECKING,
        AVAILABLE,
        MAINTENANCE
    }

    private final Screen screen;
    private final String message;
    private final boolean retryEnabled;

    private VisitorLauncherState(Screen screen, String message, boolean retryEnabled) {
        this.screen = screen;
        this.message = message;
        this.retryEnabled = retryEnabled;
    }

    public static VisitorLauncherState checking(String message) {
        return new VisitorLauncherState(Screen.CHECKING, message, false);
    }

    public static VisitorLauncherState available() {
        return new VisitorLauncherState(Screen.AVAILABLE, "", false);
    }

    public static VisitorLauncherState maintenance(String message) {
        return new VisitorLauncherState(Screen.MAINTENANCE, message, true);
    }

    public Screen getScreen() {
        return screen;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public boolean isVisitorAccessEnabled() {
        return screen == Screen.AVAILABLE;
    }
}
