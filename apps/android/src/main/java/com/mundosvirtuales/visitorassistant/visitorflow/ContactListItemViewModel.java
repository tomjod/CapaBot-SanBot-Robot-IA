package com.mundosvirtuales.visitorassistant.visitorflow;

public class ContactListItemViewModel {
    private final VisitorDtos.ContactSummary contact;
    private final String channelsLabel;
    private final String availabilityLabel;

    public ContactListItemViewModel(VisitorDtos.ContactSummary contact, String channelsLabel, String availabilityLabel) {
        this.contact = contact;
        this.channelsLabel = channelsLabel;
        this.availabilityLabel = availabilityLabel;
    }

    public VisitorDtos.ContactSummary getContact() {
        return contact;
    }

    public String getTitle() {
        return contact.getDisplayName();
    }

    public String getChannelsLabel() {
        return channelsLabel;
    }

    public String getAvailabilityLabel() {
        return availabilityLabel;
    }

    public boolean isEnabled() {
        return contact.isAvailable();
    }
}
