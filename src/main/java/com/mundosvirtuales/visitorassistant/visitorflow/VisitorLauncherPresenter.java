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
        VisitorFlowLogger.info("launcher.start", "checking backend availability");
        checkAvailability();
    }

    public void onRetry() {
        VisitorFlowLogger.info("launcher.retry", "retry requested from maintenance panel");
        checkAvailability();
    }

    private void checkAvailability() {
        VisitorFlowLogger.info("launcher.checkAvailability", "render=checking");
        view.render(VisitorLauncherState.checking(checkingMessage));
        contactCatalogGateway.fetchContacts(new ContactCatalogGateway.Callback() {
            @Override
            public void onSuccess(List<VisitorDtos.ContactSummary> contacts) {
                VisitorFlowLogger.info("launcher.available", VisitorFlowLogger.summarizeContacts(contacts));
                view.render(VisitorLauncherState.available());
            }

            @Override
            public void onError(String message) {
                VisitorFlowLogger.warn("launcher.maintenance", "reason=" + message);
                view.render(VisitorLauncherState.maintenance(maintenanceMessage));
            }
        });
    }
}
