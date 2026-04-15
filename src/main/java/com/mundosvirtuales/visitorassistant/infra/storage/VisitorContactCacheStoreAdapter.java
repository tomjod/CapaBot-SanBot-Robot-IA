package com.mundosvirtuales.visitorassistant.infra.storage;

import android.content.Context;

import com.mundosvirtuales.visitorassistant.visitorflow.SharedPreferencesVisitorContactCacheStore;

public class VisitorContactCacheStoreAdapter extends SharedPreferencesVisitorContactCacheStore {
    public VisitorContactCacheStoreAdapter(Context context) {
        super(context);
    }
}
