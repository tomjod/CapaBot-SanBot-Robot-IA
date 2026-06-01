package com.mundosvirtuales.visitorassistant.features.visitor.presentation.contact;

import com.mundosvirtuales.visitorassistant.visitorflow.VisitorDtos;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorFlowState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisitorContactUiState {

    private final String statusMessage;
    private final boolean statusVisible;
    private final String cacheHint;
    private final boolean cacheHintVisible;
    private final boolean maintenanceVisible;
    private final String maintenanceMessage;
    private final boolean listHintVisible;
    private final boolean progressVisible;
    private final boolean retryVisible;
    private final boolean contactsEnabled;
    private final boolean success;
    private final String successMessage;
    private final List<VisitorDtos.ContactSummary> contacts;
    private final VisitorFlowState.Screen screen;

    public VisitorContactUiState(String statusMessage,
                                 boolean statusVisible,
                                 String cacheHint,
                                 boolean cacheHintVisible,
                                 boolean maintenanceVisible,
                                 String maintenanceMessage,
                                 boolean listHintVisible,
                                 boolean progressVisible,
                                 boolean retryVisible,
                                 boolean contactsEnabled,
                                 boolean success,
                                 String successMessage,
                                 List<VisitorDtos.ContactSummary> contacts,
                                 VisitorFlowState.Screen screen) {
        this.statusMessage = statusMessage;
        this.statusVisible = statusVisible;
        this.cacheHint = cacheHint;
        this.cacheHintVisible = cacheHintVisible;
        this.maintenanceVisible = maintenanceVisible;
        this.maintenanceMessage = maintenanceMessage;
        this.listHintVisible = listHintVisible;
        this.progressVisible = progressVisible;
        this.retryVisible = retryVisible;
        this.contactsEnabled = contactsEnabled;
        this.success = success;
        this.successMessage = successMessage;
        this.contacts = Collections.unmodifiableList(new ArrayList<>(contacts));
        this.screen = screen;
    }

    public String getStatusMessage() { return statusMessage; }
    public boolean isStatusVisible() { return statusVisible; }
    public String getCacheHint() { return cacheHint; }
    public boolean isCacheHintVisible() { return cacheHintVisible; }
    public boolean isMaintenanceVisible() { return maintenanceVisible; }
    public String getMaintenanceMessage() { return maintenanceMessage; }
    public boolean isListHintVisible() { return listHintVisible; }
    public boolean isProgressVisible() { return progressVisible; }
    public boolean isRetryVisible() { return retryVisible; }
    public boolean isContactsEnabled() { return contactsEnabled; }
    public boolean isSuccess() { return success; }
    public String getSuccessMessage() { return successMessage; }
    public List<VisitorDtos.ContactSummary> getContacts() { return contacts; }
    public VisitorFlowState.Screen getScreen() { return screen; }
}
