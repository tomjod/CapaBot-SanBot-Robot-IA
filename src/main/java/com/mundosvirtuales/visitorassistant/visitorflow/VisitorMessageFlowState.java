package com.mundosvirtuales.visitorassistant.visitorflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisitorMessageFlowState {

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
    private final String draftMessage;
    private final boolean sendEnabled;

    private VisitorMessageFlowState(Screen screen,
                                    List<VisitorDtos.ContactSummary> contacts,
                                    String message,
                                    boolean retryEnabled,
                                    boolean showingCachedContacts,
                                    String selectedContactId,
                                    String draftMessage,
                                    boolean sendEnabled) {
        this.screen = screen;
        this.contacts = Collections.unmodifiableList(new ArrayList<>(contacts));
        this.message = message;
        this.retryEnabled = retryEnabled;
        this.showingCachedContacts = showingCachedContacts;
        this.selectedContactId = selectedContactId;
        this.draftMessage = draftMessage;
        this.sendEnabled = sendEnabled;
    }

    public static VisitorMessageFlowState loading(List<VisitorDtos.ContactSummary> contacts,
                                                  String message,
                                                  boolean showingCachedContacts,
                                                  String draftMessage) {
        return new VisitorMessageFlowState(Screen.LOADING, contacts, message, false, showingCachedContacts, null, draftMessage, false);
    }

    public static VisitorMessageFlowState ready(List<VisitorDtos.ContactSummary> contacts,
                                                String message,
                                                boolean retryEnabled,
                                                boolean showingCachedContacts,
                                                String selectedContactId,
                                                String draftMessage,
                                                boolean sendEnabled) {
        return new VisitorMessageFlowState(Screen.READY, contacts, message, retryEnabled, showingCachedContacts, selectedContactId, draftMessage, sendEnabled);
    }

    public static VisitorMessageFlowState maintenance(String message,
                                                      boolean retryEnabled,
                                                      String draftMessage) {
        return new VisitorMessageFlowState(
                Screen.MAINTENANCE,
                Collections.<VisitorDtos.ContactSummary>emptyList(),
                message,
                retryEnabled,
                false,
                null,
                draftMessage,
                false
        );
    }

    public static VisitorMessageFlowState submitting(List<VisitorDtos.ContactSummary> contacts,
                                                     String message,
                                                     String selectedContactId,
                                                     String draftMessage) {
        return new VisitorMessageFlowState(Screen.SUBMITTING, contacts, message, false, false, selectedContactId, draftMessage, false);
    }

    public static VisitorMessageFlowState success(List<VisitorDtos.ContactSummary> contacts,
                                                  String message,
                                                  String selectedContactId,
                                                  String draftMessage) {
        return new VisitorMessageFlowState(Screen.SUCCESS, contacts, message, false, false, selectedContactId, draftMessage, false);
    }

    public static VisitorMessageFlowState unavailable(List<VisitorDtos.ContactSummary> contacts,
                                                      String message,
                                                      String selectedContactId,
                                                      String draftMessage,
                                                      boolean sendEnabled) {
        return new VisitorMessageFlowState(Screen.UNAVAILABLE, contacts, message, true, false, selectedContactId, draftMessage, sendEnabled);
    }

    public static VisitorMessageFlowState failed(List<VisitorDtos.ContactSummary> contacts,
                                                 String message,
                                                 boolean retryEnabled,
                                                 boolean showingCachedContacts,
                                                 String selectedContactId,
                                                 String draftMessage,
                                                 boolean sendEnabled) {
        return new VisitorMessageFlowState(Screen.FAILED, contacts, message, retryEnabled, showingCachedContacts, selectedContactId, draftMessage, sendEnabled);
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

    public String getDraftMessage() {
        return draftMessage;
    }

    public boolean isSendEnabled() {
        return sendEnabled;
    }
}
