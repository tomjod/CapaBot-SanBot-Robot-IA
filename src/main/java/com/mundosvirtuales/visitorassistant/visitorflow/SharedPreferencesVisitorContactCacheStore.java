package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collections;
import java.util.List;

public class SharedPreferencesVisitorContactCacheStore implements VisitorContactCacheStore {

    private static final String PREFS_NAME = "visitor_contact_cache";
    private static final String KEY_CONTACTS = "contacts_json";

    private final SharedPreferences sharedPreferences;

    public SharedPreferencesVisitorContactCacheStore(Context context) {
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Override
    public List<VisitorDtos.ContactSummary> getCachedContacts() {
        String raw = sharedPreferences.getString(KEY_CONTACTS, null);
        return VisitorContactCacheSerializer.deserialize(raw);
    }

    @Override
    public void saveContacts(List<VisitorDtos.ContactSummary> contacts) {
        if (contacts == null) {
            contacts = Collections.emptyList();
        }
        sharedPreferences.edit()
                .putString(KEY_CONTACTS, VisitorContactCacheSerializer.serialize(contacts))
                .apply();
    }
}
