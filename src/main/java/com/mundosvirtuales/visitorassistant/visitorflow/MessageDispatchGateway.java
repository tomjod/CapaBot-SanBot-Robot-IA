package com.mundosvirtuales.visitorassistant.visitorflow;

public interface MessageDispatchGateway {

    void submitMessageNotification(VisitorDtos.ContactSummary contact, String message, NotificationDispatchGateway.Callback callback);
}
