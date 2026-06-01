package com.mundosvirtuales.visitorassistant.app.visitor;

import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorEntryPlan;
import com.mundosvirtuales.visitorassistant.features.visitor.presentation.start.VisitorStartUiEvent;

public final class VisitorActivityDestinationResolver {

    public enum Destination {
        VISITOR_START,
        VISITOR_CONTACT,
        VISITOR_LEAVE_MESSAGE,
        VISITOR_INFORMATION,
        LEGACY_DIALOG,
        LEGACY_BASE
    }

    private VisitorActivityDestinationResolver() {
    }

    public static Destination resolveEntryTarget(VisitorEntryPlan.Target target) {
        if (target == VisitorEntryPlan.Target.GUIDED_VISITOR_FLOW) {
            return Destination.VISITOR_START;
        }
        return Destination.LEGACY_DIALOG;
    }

    public static Destination resolveStartTarget(VisitorStartUiEvent event) {
        if (event.getTarget() == VisitorStartUiEvent.Target.CONTACT_FLOW) {
            return Destination.VISITOR_CONTACT;
        }
        if (event.getTarget() == VisitorStartUiEvent.Target.MESSAGE_FLOW) {
            return Destination.VISITOR_LEAVE_MESSAGE;
        }
        if (event.getTarget() == VisitorStartUiEvent.Target.INFORMATION_FLOW) {
            return Destination.VISITOR_INFORMATION;
        }
        return Destination.LEGACY_BASE;
    }
}
