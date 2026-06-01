package com.mundosvirtuales.visitorassistant.visitorflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisitorFlowState {

    public enum Screen {
        LOADING,
        READY,
        MAINTENANCE,
        SUBMITTING,
        SUCCESS,
        UNAVAILABLE,
        FAILED
    }

    private final Screen screen;
    private final List<VisitorDtos.ContactSummary> contacts;
    private final String message;
    private final boolean retryEnabled;
    private final boolean showingCachedContacts;
    private final String selectedContactId;

    private VisitorFlowState(Screen screen,
                             List<VisitorDtos.ContactSummary> contacts,
                             String message,
                             boolean retryEnabled,
                             boolean showingCachedContacts,
                             String selectedContactId) {
        this.screen = screen;
        this.contacts = Collections.unmodifiableList(new ArrayList<>(contacts));
        this.message = message;
        this.retryEnabled = retryEnabled;
        this.showingCachedContacts = showingCachedContacts;
        this.selectedContactId = selectedContactId;
    }

    public static VisitorFlowState loading(String message) {
        return loading(Collections.<VisitorDtos.ContactSummary>emptyList(), message, false);
    }

    public static VisitorFlowState loading(List<VisitorDtos.ContactSummary> contacts, String message, boolean showingCachedContacts) {
        return new VisitorFlowState(Screen.LOADING, contacts, message, false, showingCachedContacts, null);
    }

    public static VisitorFlowState ready(List<VisitorDtos.ContactSummary> contacts, String message) {
        return ready(contacts, message, false);
    }

    public static VisitorFlowState ready(List<VisitorDtos.ContactSummary> contacts, String message, boolean retryEnabled) {
        return ready(contacts, message, retryEnabled, false);
    }

    public static VisitorFlowState ready(List<VisitorDtos.ContactSummary> contacts,
                                         String message,
                                         boolean retryEnabled,
                                         boolean showingCachedContacts) {
        return new VisitorFlowState(Screen.READY, contacts, message, retryEnabled, showingCachedContacts, null);
    }

    public static VisitorFlowState maintenance(String message, boolean retryEnabled) {
        return new VisitorFlowState(Screen.MAINTENANCE, Collections.<VisitorDtos.ContactSummary>emptyList(), message, retryEnabled, false, null);
    }

    public static VisitorFlowState submitting(List<VisitorDtos.ContactSummary> contacts, String message, String selectedContactId) {
        return new VisitorFlowState(Screen.SUBMITTING, contacts, message, false, false, selectedContactId);
    }

    public static VisitorFlowState success(List<VisitorDtos.ContactSummary> contacts, String message, String selectedContactId) {
        return new VisitorFlowState(Screen.SUCCESS, contacts, message, false, false, selectedContactId);
    }

    public static VisitorFlowState unavailable(List<VisitorDtos.ContactSummary> contacts, String message, String selectedContactId) {
        return new VisitorFlowState(Screen.UNAVAILABLE, contacts, message, true, false, selectedContactId);
    }

    public static VisitorFlowState failed(List<VisitorDtos.ContactSummary> contacts, String message, boolean retryEnabled, String selectedContactId) {
        return failed(contacts, message, retryEnabled, false, selectedContactId);
    }

    public static VisitorFlowState failed(List<VisitorDtos.ContactSummary> contacts,
                                          String message,
                                          boolean retryEnabled,
                                          boolean showingCachedContacts,
                                          String selectedContactId) {
        return new VisitorFlowState(Screen.FAILED, contacts, message, retryEnabled, showingCachedContacts, selectedContactId);
    }

    public Screen getScreen() {
        return screen;
    }

    public List<VisitorDtos.ContactSummary> getContacts() {
        return contacts;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public boolean isShowingCachedContacts() {
        return showingCachedContacts;
    }

    public String getSelectedContactId() {
        return selectedContactId;
    }
}
