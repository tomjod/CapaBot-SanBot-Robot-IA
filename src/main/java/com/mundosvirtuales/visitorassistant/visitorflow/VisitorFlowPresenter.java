package com.mundosvirtuales.visitorassistant.visitorflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisitorFlowPresenter {

    public interface View {
        void render(VisitorFlowState state);
    }

    private final View view;
    private final ContactCatalogGateway contactCatalogGateway;
    private final NotificationDispatchGateway notificationDispatchGateway;
    private final VisitorContactCacheStore contactCacheStore;
    private final RobotSpeechPort robotSpeechPort;
    private final String loadingMessage;
    private final String cachedLoadingMessage;
    private final String readyMessage;
    private final String unavailableMessage;
    private final String failedMessage;
    private final String loadFailedMessage;
    private final String maintenanceMessage;
    private final String emptyMessage;
    private final String successTemplate;

    private List<VisitorDtos.ContactSummary> contacts = Collections.emptyList();
    private VisitorDtos.ContactSummary lastSelectedContact;
    private VisitorFlowState currentState;

    public VisitorFlowPresenter(View view,
                                ContactCatalogGateway contactCatalogGateway,
                                NotificationDispatchGateway notificationDispatchGateway,
                                VisitorContactCacheStore contactCacheStore,
                                RobotSpeechPort robotSpeechPort,
                                String loadingMessage,
                                String cachedLoadingMessage,
                                String readyMessage,
                                String unavailableMessage,
                                String failedMessage,
                                String loadFailedMessage,
                                String maintenanceMessage,
                                String emptyMessage,
                                String successTemplate) {
        this.view = view;
        this.contactCatalogGateway = contactCatalogGateway;
        this.notificationDispatchGateway = notificationDispatchGateway;
        this.contactCacheStore = contactCacheStore;
        this.robotSpeechPort = robotSpeechPort;
        this.loadingMessage = loadingMessage;
        this.cachedLoadingMessage = cachedLoadingMessage;
        this.readyMessage = readyMessage;
        this.unavailableMessage = unavailableMessage;
        this.failedMessage = failedMessage;
        this.loadFailedMessage = loadFailedMessage;
        this.maintenanceMessage = maintenanceMessage;
        this.emptyMessage = emptyMessage;
        this.successTemplate = successTemplate;
    }

    public void start() {
        loadContacts();
    }

    public void onRetry() {
        if (lastSelectedContact != null && currentState != null && currentState.getScreen() == VisitorFlowState.Screen.FAILED) {
            submitContact(lastSelectedContact);
            return;
        }
        loadContacts();
    }

    public void onContactSelected(VisitorDtos.ContactSummary contact) {
        if (contact == null) {
            return;
        }

        if (!contact.isAvailable()) {
            currentState = VisitorFlowState.unavailable(contacts, unavailableMessage, contact.getId());
            view.render(currentState);
            robotSpeechPort.speak(unavailableMessage);
            return;
        }

        submitContact(contact);
    }

    private void loadContacts() {
        lastSelectedContact = null;
        contacts = Collections.emptyList();
        currentState = VisitorFlowState.loading(loadingMessage);
        view.render(currentState);

        contactCatalogGateway.fetchContacts(new ContactCatalogGateway.Callback() {
            @Override
            public void onSuccess(List<VisitorDtos.ContactSummary> contactsResponse) {
                contacts = contactsResponse == null ? Collections.<VisitorDtos.ContactSummary>emptyList() : new ArrayList<>(contactsResponse);
                if (contactCacheStore != null) {
                    contactCacheStore.saveContacts(contacts);
                }
                String message = contacts.isEmpty() ? emptyMessage : readyMessage;
                currentState = VisitorFlowState.ready(contacts, message, contacts.isEmpty(), false);
                view.render(currentState);
                robotSpeechPort.speak(message);
            }

            @Override
            public void onError(String message) {
                contacts = Collections.emptyList();
                currentState = VisitorFlowState.maintenance(maintenanceMessage, true);
                view.render(currentState);
                robotSpeechPort.speak(currentState.getMessage());
            }
        });
    }

    private void submitContact(final VisitorDtos.ContactSummary contact) {
        lastSelectedContact = contact;
        currentState = VisitorFlowState.submitting(contacts, contact.getDisplayName(), contact.getId());
        view.render(currentState);

        notificationDispatchGateway.submitNotification(contact, new NotificationDispatchGateway.Callback() {
            @Override
            public void onSuccess(VisitorDtos.NotificationResult result) {
                String status = result != null ? result.getStatus() : null;
                if ("accepted".equals(status) || "delivered_or_queued".equals(status)) {
                    String message = result != null && result.getDetail() != null
                            ? safeSubmissionMessage(result.getDetail())
                            : String.format(successTemplate, contact.getDisplayName());
                    currentState = VisitorFlowState.success(contacts, message, contact.getId());
                    view.render(currentState);
                    robotSpeechPort.speak(message);
                    return;
                }

                if ("unavailable".equals(status)) {
                    String message = result != null && result.getDetail() != null
                            ? safeUnavailableMessage(result.getDetail())
                            : unavailableMessage;
                    currentState = VisitorFlowState.unavailable(contacts, message, contact.getId());
                    view.render(currentState);
                    robotSpeechPort.speak(message);
                    return;
                }

                String message = result != null && result.getDetail() != null
                        ? safeSubmissionMessage(result.getDetail())
                        : failedMessage;
                boolean retryable = result != null && result.isRetryable();
                if (retryable && shouldSwitchToMaintenance(result != null ? result.getDetail() : null)) {
                    currentState = VisitorFlowState.maintenance(maintenanceMessage, true);
                    view.render(currentState);
                    robotSpeechPort.speak(currentState.getMessage());
                    return;
                }
                currentState = VisitorFlowState.failed(contacts, message, retryable, contact.getId());
                view.render(currentState);
                robotSpeechPort.speak(message);
            }

            @Override
            public void onError(String message, boolean retryable) {
                if (retryable || shouldSwitchToMaintenance(message)) {
                    currentState = VisitorFlowState.maintenance(maintenanceMessage, true);
                    view.render(currentState);
                    robotSpeechPort.speak(currentState.getMessage());
                    return;
                }
                currentState = VisitorFlowState.failed(contacts, safeSubmissionMessage(message), retryable, contact.getId());
                view.render(currentState);
                robotSpeechPort.speak(currentState.getMessage());
            }
        });
    }

    private String safeLoadMessage(String message) {
        String normalized = VisitorSupportMessageSanitizer.normalize(message);
        if (normalized == null) {
            return loadFailedMessage;
        }
        return VisitorSupportMessageSanitizer.looksTechnicalFailure(normalized) ? loadFailedMessage : normalized;
    }

    private String safeSubmissionMessage(String message) {
        String normalized = VisitorSupportMessageSanitizer.normalize(message);
        if (normalized == null) {
            return failedMessage;
        }
        return VisitorSupportMessageSanitizer.looksTechnicalFailure(normalized) ? failedMessage : normalized;
    }

    private String safeUnavailableMessage(String message) {
        String normalized = VisitorSupportMessageSanitizer.normalize(message);
        if (normalized == null) {
            return unavailableMessage;
        }
        return VisitorSupportMessageSanitizer.looksTechnicalFailure(normalized) ? unavailableMessage : normalized;
    }

    private boolean shouldSwitchToMaintenance(String message) {
        return VisitorSupportMessageSanitizer.normalize(message) == null
                || VisitorSupportMessageSanitizer.looksTechnicalFailure(message);
    }
}
