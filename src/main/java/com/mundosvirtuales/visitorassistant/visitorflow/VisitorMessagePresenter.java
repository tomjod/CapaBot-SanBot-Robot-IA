package com.mundosvirtuales.visitorassistant.visitorflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisitorMessagePresenter {

    public interface View {
        void render(VisitorMessageFlowState state);
    }

    static final int MAX_MESSAGE_LENGTH = 280;

    private final View view;
    private final ContactCatalogGateway contactCatalogGateway;
    private final MessageDispatchGateway messageDispatchGateway;
    private final VisitorContactCacheStore contactCacheStore;
    private final RobotSpeechPort robotSpeechPort;
    private final String loadingMessage;
    private final String cachedLoadingMessage;
    private final String readyMessage;
    private final String selectedTemplate;
    private final String unavailableMessage;
    private final String failedMessage;
    private final String maintenanceMessage;
    private final String emptyContactsMessage;
    private final String validationContactMessage;
    private final String validationTextMessage;
    private final String validationLengthMessage;
    private final String successTemplate;

    private List<VisitorDtos.ContactSummary> contacts = Collections.emptyList();
    private VisitorDtos.ContactSummary selectedContact;
    private String draftMessage = "";
    private VisitorMessageFlowState currentState;

    public VisitorMessagePresenter(View view,
                                   ContactCatalogGateway contactCatalogGateway,
                                   MessageDispatchGateway messageDispatchGateway,
                                   VisitorContactCacheStore contactCacheStore,
                                   RobotSpeechPort robotSpeechPort,
                                   String loadingMessage,
                                   String cachedLoadingMessage,
                                    String readyMessage,
                                    String selectedTemplate,
                                    String unavailableMessage,
                                    String failedMessage,
                                    String maintenanceMessage,
                                    String emptyContactsMessage,
                                    String validationContactMessage,
                                    String validationTextMessage,
                                   String validationLengthMessage,
                                   String successTemplate) {
        this.view = view;
        this.contactCatalogGateway = contactCatalogGateway;
        this.messageDispatchGateway = messageDispatchGateway;
        this.contactCacheStore = contactCacheStore;
        this.robotSpeechPort = robotSpeechPort;
        this.loadingMessage = loadingMessage;
        this.cachedLoadingMessage = cachedLoadingMessage;
        this.readyMessage = readyMessage;
        this.selectedTemplate = selectedTemplate;
        this.unavailableMessage = unavailableMessage;
        this.failedMessage = failedMessage;
        this.maintenanceMessage = maintenanceMessage;
        this.emptyContactsMessage = emptyContactsMessage;
        this.validationContactMessage = validationContactMessage;
        this.validationTextMessage = validationTextMessage;
        this.validationLengthMessage = validationLengthMessage;
        this.successTemplate = successTemplate;
    }

    public void start() {
        loadContacts();
    }

    public void onContactSelected(VisitorDtos.ContactSummary contact) {
        if (contact == null) {
            return;
        }

        selectedContact = contact;
        if (!contact.isAvailable()) {
            currentState = VisitorMessageFlowState.unavailable(
                    contacts,
                    unavailableMessage,
                    contact.getId(),
                    draftMessage,
                    isDraftValid(draftMessage)
            );
            view.render(currentState);
            robotSpeechPort.speak(unavailableMessage);
            return;
        }

        renderReady(String.format(selectedTemplate, contact.getDisplayName()), false, false);
    }

    public void onMessageChanged(String message) {
        draftMessage = message == null ? "" : message;
        String currentMessage = currentState != null ? currentState.getMessage() : readyMessage;
        renderReady(currentMessage, currentState != null && currentState.isRetryEnabled(), currentState != null && currentState.isShowingCachedContacts());
    }

    public void onSubmit() {
        if (selectedContact == null) {
            renderReady(validationContactMessage, false, false);
            robotSpeechPort.speak(validationContactMessage);
            return;
        }

        if (!selectedContact.isAvailable()) {
            currentState = VisitorMessageFlowState.unavailable(contacts, unavailableMessage, selectedContact.getId(), draftMessage, false);
            view.render(currentState);
            robotSpeechPort.speak(unavailableMessage);
            return;
        }

        String validationError = validateDraft(draftMessage);
        if (validationError != null) {
            renderReady(validationError, false, false);
            robotSpeechPort.speak(validationError);
            return;
        }

        submitMessage(selectedContact, draftMessage.trim());
    }

    public void onRetry() {
        if (selectedContact != null && currentState != null && currentState.getScreen() == VisitorMessageFlowState.Screen.FAILED && isDraftValid(draftMessage)) {
            submitMessage(selectedContact, draftMessage.trim());
            return;
        }
        loadContacts();
    }

    private void loadContacts() {
        selectedContact = null;
        contacts = Collections.emptyList();
        currentState = VisitorMessageFlowState.loading(
                Collections.<VisitorDtos.ContactSummary>emptyList(),
                loadingMessage,
                false,
                draftMessage
        );
        view.render(currentState);

        contactCatalogGateway.fetchContacts(new ContactCatalogGateway.Callback() {
            @Override
            public void onSuccess(List<VisitorDtos.ContactSummary> contactsResponse) {
                contacts = contactsResponse == null ? Collections.<VisitorDtos.ContactSummary>emptyList() : new ArrayList<>(contactsResponse);
                if (contactCacheStore != null) {
                    contactCacheStore.saveContacts(contacts);
                }

                String message = contacts.isEmpty() ? emptyContactsMessage : readyMessage;
                currentState = VisitorMessageFlowState.ready(contacts, message, contacts.isEmpty(), false, null, draftMessage, false);
                view.render(currentState);
                robotSpeechPort.speak(message);
            }

            @Override
            public void onError(String message) {
                contacts = Collections.emptyList();
                currentState = VisitorMessageFlowState.maintenance(maintenanceMessage, true, draftMessage);
                view.render(currentState);
                robotSpeechPort.speak(currentState.getMessage());
            }
        });
    }

    private void submitMessage(final VisitorDtos.ContactSummary contact, final String normalizedMessage) {
        currentState = VisitorMessageFlowState.submitting(
                contacts,
                String.format("Enviando mensaje a %s…", contact.getDisplayName()),
                contact.getId(),
                normalizedMessage
        );
        view.render(currentState);

        messageDispatchGateway.submitMessageNotification(contact, normalizedMessage, new NotificationDispatchGateway.Callback() {
            @Override
            public void onSuccess(VisitorDtos.NotificationResult result) {
                String status = result != null ? result.getStatus() : null;
                if ("accepted".equals(status) || "delivered_or_queued".equals(status)) {
                    String message = result != null && result.getDetail() != null
                            ? safeSuccessMessage(result.getDetail(), contact.getDisplayName())
                            : String.format(successTemplate, contact.getDisplayName());
                    currentState = VisitorMessageFlowState.success(contacts, message, contact.getId(), normalizedMessage);
                    view.render(currentState);
                    robotSpeechPort.speak(message);
                    return;
                }

                if ("unavailable".equals(status)) {
                    String message = result != null && result.getDetail() != null
                            ? safeUnavailableMessage(result.getDetail())
                            : unavailableMessage;
                    currentState = VisitorMessageFlowState.unavailable(contacts, message, contact.getId(), normalizedMessage, false);
                    view.render(currentState);
                    robotSpeechPort.speak(message);
                    return;
                }

                String message = result != null && result.getDetail() != null ? safeMessage(result.getDetail()) : failedMessage;
                boolean retryable = result != null && result.isRetryable();
                if (retryable && shouldSwitchToMaintenance(result != null ? result.getDetail() : null)) {
                    currentState = VisitorMessageFlowState.maintenance(maintenanceMessage, true, normalizedMessage);
                    view.render(currentState);
                    robotSpeechPort.speak(currentState.getMessage());
                    return;
                }
                currentState = VisitorMessageFlowState.failed(contacts, message, retryable, false, contact.getId(), normalizedMessage, true);
                view.render(currentState);
                robotSpeechPort.speak(message);
            }

            @Override
            public void onError(String message, boolean retryable) {
                if (retryable || shouldSwitchToMaintenance(message)) {
                    currentState = VisitorMessageFlowState.maintenance(maintenanceMessage, true, normalizedMessage);
                    view.render(currentState);
                    robotSpeechPort.speak(currentState.getMessage());
                    return;
                }
                currentState = VisitorMessageFlowState.failed(contacts, safeMessage(message), retryable, false, contact.getId(), normalizedMessage, true);
                view.render(currentState);
                robotSpeechPort.speak(currentState.getMessage());
            }
        });
    }

    private void renderReady(String message, boolean retryEnabled, boolean showingCachedContacts) {
        currentState = VisitorMessageFlowState.ready(
                contacts,
                message,
                retryEnabled,
                showingCachedContacts,
                selectedContact != null ? selectedContact.getId() : null,
                draftMessage,
                selectedContact != null && selectedContact.isAvailable() && isDraftValid(draftMessage)
        );
        view.render(currentState);
    }

    private boolean isDraftValid(String message) {
        return validateDraft(message) == null;
    }

    private String validateDraft(String message) {
        String normalized = message == null ? "" : message.trim();
        if (normalized.isEmpty()) {
            return validationTextMessage;
        }
        if (normalized.length() > MAX_MESSAGE_LENGTH) {
            return validationLengthMessage;
        }
        return null;
    }

    private String safeMessage(String message) {
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

    private String safeSuccessMessage(String message, String displayName) {
        String normalized = VisitorSupportMessageSanitizer.normalize(message);
        if (normalized == null || VisitorSupportMessageSanitizer.looksTechnicalFailure(normalized)) {
            return String.format(successTemplate, displayName);
        }
        return normalized;
    }

    private boolean shouldSwitchToMaintenance(String message) {
        return VisitorSupportMessageSanitizer.normalize(message) == null
                || VisitorSupportMessageSanitizer.looksTechnicalFailure(message);
    }
}
