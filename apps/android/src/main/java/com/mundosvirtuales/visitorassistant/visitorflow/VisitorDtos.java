package com.mundosvirtuales.visitorassistant.visitorflow;

import com.google.gson.annotations.SerializedName;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class VisitorDtos {

    public static class ContactSummary {
        private final String id;
        private final String displayName;
        private final boolean telegramAvailable;
        private final boolean emailAvailable;
        private final boolean available;

        public ContactSummary(String id, String displayName, boolean telegramAvailable, boolean emailAvailable, boolean available) {
            this.id = id;
            this.displayName = displayName;
            this.telegramAvailable = telegramAvailable;
            this.emailAvailable = emailAvailable;
            this.available = available;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isTelegramAvailable() {
            return telegramAvailable;
        }

        public boolean isEmailAvailable() {
            return emailAvailable;
        }

        public boolean isAvailable() {
            return available;
        }
    }

    public static class NotificationResult {
        private final String status;
        private final String telegramStatus;
        private final String emailStatus;
        private final boolean retryable;
        private final String detail;

        public NotificationResult(String status, String telegramStatus, String emailStatus, boolean retryable, String detail) {
            this.status = status;
            this.telegramStatus = telegramStatus;
            this.emailStatus = emailStatus;
            this.retryable = retryable;
            this.detail = detail;
        }

        public String getStatus() {
            return status;
        }

        public String getTelegramStatus() {
            return telegramStatus;
        }

        public String getEmailStatus() {
            return emailStatus;
        }

        public boolean isRetryable() {
            return retryable;
        }

        public String getDetail() {
            return detail;
        }
    }

    public static class ContactDto {
        @SerializedName("id")
        String id;

        @SerializedName("display_name")
        String displayName;

        @SerializedName("channels")
        ChannelsDto channels;

        @SerializedName("available")
        boolean available;

        ContactSummary toSummary() {
            boolean telegram = channels != null && channels.telegram;
            boolean email = channels != null && channels.email;
            return new ContactSummary(id, displayName, telegram, email, available);
        }
    }

    public static class ChannelsDto {
        @SerializedName("telegram")
        boolean telegram;

        @SerializedName("email")
        boolean email;
    }

    public static class NotificationRequestDto {
        @SerializedName("contact_id")
        String contactId;

        @SerializedName("device_id")
        String deviceId;

        @SerializedName("requested_at")
        String requestedAt;

        @SerializedName("visitor_name")
        String visitorName;

        @SerializedName("location")
        String location;

        @SerializedName("reason")
        String reason;

        @SerializedName("message")
        String message;

        public NotificationRequestDto(String contactId, String deviceId, String visitorName, String location) {
            this(contactId, deviceId, visitorName, location, null, null);
        }

        public NotificationRequestDto(String contactId,
                                      String deviceId,
                                      String visitorName,
                                      String location,
                                      String reason,
                                      String message) {
            this.contactId = contactId;
            this.deviceId = deviceId;
            this.requestedAt = isoNow();
            this.visitorName = normalizedOrNull(visitorName);
            this.location = normalizedOrNull(location);
            this.reason = normalizedOrNull(reason);
            this.message = normalizedOrNull(message);
        }

        private static String isoNow() {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.format(new Date());
        }

        private static String normalizedOrNull(String value) {
            if (value == null) {
                return null;
            }
            String normalized = value.trim();
            return normalized.isEmpty() ? null : normalized;
        }
    }

    public static class NotificationResponseDto {
        @SerializedName("status")
        String status;

        @SerializedName("channels")
        ChannelsStatusDto channels;

        @SerializedName("retryable")
        boolean retryable;

        @SerializedName("detail")
        String detail;

        NotificationResult toResult() {
            return new NotificationResult(
                    status,
                    channels != null ? channels.telegram : null,
                    channels != null ? channels.email : null,
                    retryable,
                    detail
            );
        }
    }

    public static class ChannelsStatusDto {
        @SerializedName("telegram")
        String telegram;

        @SerializedName("email")
        String email;
    }

    static class ErrorResponseDto {
        @SerializedName("detail")
        String detail;
    }
}
