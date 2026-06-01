package com.mundosvirtuales.visitorassistant.visitorflow;

import android.util.Log;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class VisitorFlowLogger {

    private static final String TAG = "VisitorFlow";
    private static final AtomicLong REQUEST_COUNTER = new AtomicLong(1000L);

    private VisitorFlowLogger() {
    }

    public static long nextRequestId() {
        return REQUEST_COUNTER.incrementAndGet();
    }

    public static void info(String event, String details) {
        try {
            Log.i(TAG, buildMessage(event, details));
        } catch (RuntimeException ignored) {
            // Android stubs throw in local JVM tests; logging must never break visitor flows.
        }
    }

    public static void warn(String event, String details) {
        try {
            Log.w(TAG, buildMessage(event, details));
        } catch (RuntimeException ignored) {
            // Android stubs throw in local JVM tests; logging must never break visitor flows.
        }
    }

    public static void error(String event, String details, Throwable throwable) {
        try {
            Log.e(TAG, buildMessage(event, details), throwable);
        } catch (RuntimeException ignored) {
            // Android stubs throw in local JVM tests; logging must never break visitor flows.
        }
    }

    public static String summarizeBaseConfig(String baseUrl, String deviceId, String location, String defaultVisitorName) {
        return "baseUrl=" + safe(baseUrl)
                + ", deviceId=" + safe(deviceId)
                + ", location=" + safe(location)
                + ", defaultVisitorName=" + safe(defaultVisitorName);
    }

    public static String summarizeContacts(List<VisitorDtos.ContactSummary> contacts) {
        if (contacts == null) {
            return "contacts=null";
        }
        return "contacts=" + contacts.size();
    }

    public static String summarizeContact(VisitorDtos.ContactSummary contact) {
        if (contact == null) {
            return "contact=null";
        }
        return "contactId=" + safe(contact.getId())
                + ", displayName=" + safe(contact.getDisplayName())
                + ", available=" + contact.isAvailable();
    }

    public static String summarizeNotification(VisitorDtos.NotificationRequestDto request) {
        if (request == null) {
            return "notificationRequest=null";
        }
        return "contactId=" + safe(request.contactId)
                + ", deviceId=" + safe(request.deviceId)
                + ", visitorName=" + safe(request.visitorName)
                + ", location=" + safe(request.location)
                + ", reason=" + safe(request.reason)
                + ", hasMessage=" + !isNullOrEmpty(request.message);
    }

    private static String buildMessage(String event, String details) {
        return event + " | " + (details == null ? "" : details);
    }

    private static String safe(String value) {
        if (isNullOrEmpty(value)) {
            return "<empty>";
        }
        return value;
    }

    private static boolean isNullOrEmpty(String value) {
        return value == null || value.length() == 0;
    }
}
