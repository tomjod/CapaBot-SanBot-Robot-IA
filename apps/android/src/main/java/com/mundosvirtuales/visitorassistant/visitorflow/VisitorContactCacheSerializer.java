package com.mundosvirtuales.visitorassistant.visitorflow;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class VisitorContactCacheSerializer {

    private static final Gson GSON = new Gson();
    private static final Type PAYLOAD_TYPE = new TypeToken<CachePayload>() { }.getType();

    private VisitorContactCacheSerializer() {
    }

    public static String serialize(List<VisitorDtos.ContactSummary> contacts) {
        CachePayload payload = new CachePayload();
        payload.contacts = new ArrayList<>();
        if (contacts != null) {
            for (VisitorDtos.ContactSummary contact : contacts) {
                if (contact == null) {
                    continue;
                }
                payload.contacts.add(new CachedContact(contact));
            }
        }
        return GSON.toJson(payload, PAYLOAD_TYPE);
    }

    public static List<VisitorDtos.ContactSummary> deserialize(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            CachePayload payload = GSON.fromJson(raw, PAYLOAD_TYPE);
            if (payload == null || payload.contacts == null || payload.contacts.isEmpty()) {
                return Collections.emptyList();
            }

            List<VisitorDtos.ContactSummary> contacts = new ArrayList<>();
            for (CachedContact cachedContact : payload.contacts) {
                if (cachedContact == null || cachedContact.id == null || cachedContact.displayName == null) {
                    continue;
                }
                contacts.add(cachedContact.toSummary());
            }
            return contacts;
        } catch (RuntimeException exception) {
            return Collections.emptyList();
        }
    }

    private static class CachePayload {
        @SerializedName("contacts")
        List<CachedContact> contacts;
    }

    private static class CachedContact {
        @SerializedName("id")
        String id;

        @SerializedName("display_name")
        String displayName;

        @SerializedName("telegram_available")
        boolean telegramAvailable;

        @SerializedName("email_available")
        boolean emailAvailable;

        @SerializedName("available")
        boolean available;

        CachedContact(VisitorDtos.ContactSummary contact) {
            this.id = contact.getId();
            this.displayName = contact.getDisplayName();
            this.telegramAvailable = contact.isTelegramAvailable();
            this.emailAvailable = contact.isEmailAvailable();
            this.available = contact.isAvailable();
        }

        VisitorDtos.ContactSummary toSummary() {
            return new VisitorDtos.ContactSummary(id, displayName, telegramAvailable, emailAvailable, available);
        }
    }
}
