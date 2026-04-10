package com.mundosvirtuales.visitorassistant.visitorflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

final class VisitorContactOrdering {

    private VisitorContactOrdering() {
    }

    static List<VisitorDtos.ContactSummary> sort(List<VisitorDtos.ContactSummary> contacts) {
        List<VisitorDtos.ContactSummary> sortedContacts = new ArrayList<>(contacts);
        Collections.sort(sortedContacts, new Comparator<VisitorDtos.ContactSummary>() {
            @Override
            public int compare(VisitorDtos.ContactSummary left, VisitorDtos.ContactSummary right) {
                if (left.isAvailable() != right.isAvailable()) {
                    return left.isAvailable() ? -1 : 1;
                }
                String leftName = left.getDisplayName() == null ? "" : left.getDisplayName();
                String rightName = right.getDisplayName() == null ? "" : right.getDisplayName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });
        return sortedContacts;
    }
}
