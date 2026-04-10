package com.mundosvirtuales.visitorassistant.visitorflow;

import java.util.List;

public interface ContactCatalogGateway {

    interface Callback {
        void onSuccess(List<VisitorDtos.ContactSummary> contacts);

        void onError(String message);
    }

    void fetchContacts(Callback callback);
}
