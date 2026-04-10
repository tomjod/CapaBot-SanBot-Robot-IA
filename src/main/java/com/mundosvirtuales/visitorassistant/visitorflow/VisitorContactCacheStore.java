package com.mundosvirtuales.visitorassistant.visitorflow;

import java.util.List;

public interface VisitorContactCacheStore {

    List<VisitorDtos.ContactSummary> getCachedContacts();

    void saveContacts(List<VisitorDtos.ContactSummary> contacts);
}
