package com.mundosvirtuales.visitorassistant.infra.api;

import com.mundosvirtuales.visitorassistant.features.visitor.data.VisitorAvailabilityGateway;
import com.mundosvirtuales.visitorassistant.visitorflow.ContactCatalogGateway;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorDtos;

import java.util.List;

public class VisitorApiAvailabilityGateway implements VisitorAvailabilityGateway {

    private final ContactCatalogGateway contactCatalogGateway;

    public VisitorApiAvailabilityGateway(ContactCatalogGateway contactCatalogGateway) {
        this.contactCatalogGateway = contactCatalogGateway;
    }

    @Override
    public void checkAvailability(Callback callback) {
        contactCatalogGateway.fetchContacts(new ContactCatalogGateway.Callback() {
            @Override
            public void onSuccess(List<VisitorDtos.ContactSummary> contacts) {
                callback.onAvailable();
            }

            @Override
            public void onError(String message) {
                callback.onUnavailable(message);
            }
        });
    }
}
