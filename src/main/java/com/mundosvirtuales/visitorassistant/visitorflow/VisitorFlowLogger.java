package com.mundosvirtuales.visitorassistant.visitorflow;

import android.text.TextUtils;
import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

final class VisitorFlowLogger {

    private static final String TAG = "VisitorFlow";
    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(1000L);

    private VisitorFlowLogger() {
    }

    static long nextRequestId() {
        return REQUEST_COUNTER.incrementAndGet();
    }

    static void info(String event, String details) {
        Log.i(TAG, buildMessage(event, details));
    }

    static void warn(String event, String details) {
        Log.w(TAG, buildMessage(event, details));
    }

    static void error(String event, String details, Throwable throwable) {
        Log.e(TAG, buildMessage(event, details), throwable);
    }

    static String summarizeBaseConfig(String baseUrl, String deviceId, String location, String defaultVisitorName) {
        return "baseUrl=" + safe(baseUrl)
                + ", deviceId=" + safe(deviceId)
                + ", location=" + safe(location)
                + ", defaultVisitorName=" + safe(defaultVisitorName);
    }

    static String summarizeContacts(List<VisitorDtos.ContactSummary> contacts) {
        if (contacts == null) {
            return "contacts=null";
        }
        return "contacts=" + contacts.size();
    }

    static String summarizeContact(VisitorDtos.ContactSummary contact) {
        if (contact == null) {
            return "contact=null";
        }
        return "contactId=" + safe(contact.getId())
                + ", displayName=" + safe(contact.getDisplayName())
                + ", available=" + contact.isAvailable();
    }

    static String summarizeNotification(VisitorDtos.NotificationRequestDto request) {
        if (request == null) {
            return "notificationRequest=null";
        }
        return "contactId=" + safe(request.contactId)
                + ", deviceId=" + safe(request.deviceId)
                + ", visitorName=" + safe(request.visitorName)
                + ", location=" + safe(request.location)
                + ", reason=" + safe(request.reason)
                + ", hasMessage=" + !TextUtils.isEmpty(request.message);
    }

    private static String buildMessage(String event, String details) {
        return event + " | " + (details == null ? "" : details);
    }

    private static String safe(String value) {
        if (TextUtils.isEmpty(value)) {
            return "<empty>";
        }
        return value;
    }
}
