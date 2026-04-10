package com.mundosvirtuales.visitorassistant.visitorflow;

public interface NotificationDispatchGateway {

    interface Callback {
        void onSuccess(VisitorDtos.NotificationResult result);

        void onError(String message, boolean retryable);
    }

    void submitNotification(VisitorDtos.ContactSummary contact, Callback callback);
}
