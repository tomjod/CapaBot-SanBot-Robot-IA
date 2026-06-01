package com.mundosvirtuales.visitorassistant.features.visitor.presentation.contact;

import com.mundosvirtuales.visitorassistant.visitorflow.VisitorDtos;

public class VisitorContactUiEvent {

    public enum Type {
        PROMPT_VISITOR_NAME,
        INVALID_VISITOR_NAME
    }

    private final Type type;
    private final VisitorDtos.ContactSummary contact;

    public VisitorContactUiEvent(Type type, VisitorDtos.ContactSummary contact) {
        this.type = type;
        this.contact = contact;
    }

    public Type getType() {
        return type;
    }

    public VisitorDtos.ContactSummary getContact() {
        return contact;
    }
}
