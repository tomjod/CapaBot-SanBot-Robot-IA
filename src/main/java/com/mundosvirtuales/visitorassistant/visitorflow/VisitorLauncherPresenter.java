package com.mundosvirtuales.visitorassistant.visitorflow;

import java.util.List;

public class VisitorLauncherPresenter {

    public interface View {
        void render(VisitorLauncherState state);
    }

    private final View view;
    private final ContactCatalogGateway contactCatalogGateway;
    private final String checkingMessage;
    private final String maintenanceMessage;

    public VisitorLauncherPresenter(View view,
                                    ContactCatalogGateway contactCatalogGateway,
                                    String checkingMessage,
                                    String maintenanceMessage) {
        this.view = view;
        this.contactCatalogGateway = contactCatalogGateway;
        this.checkingMessage = checkingMessage;
        this.maintenanceMessage = maintenanceMessage;
    }

    public void start() {
        checkAvailability();
    }

    public void onRetry() {
        checkAvailability();
    }

    private void checkAvailability() {
        view.render(VisitorLauncherState.checking(checkingMessage));
        contactCatalogGateway.fetchContacts(new ContactCatalogGateway.Callback() {
            @Override
            public void onSuccess(List<VisitorDtos.ContactSummary> contacts) {
                view.render(VisitorLauncherState.available());
            }

            @Override
            public void onError(String message) {
                view.render(VisitorLauncherState.maintenance(maintenanceMessage));
            }
        });
    }
}
