package com.mundosvirtuales.visitorassistant.app.visitor;

import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorEntryPlan;
import com.mundosvirtuales.visitorassistant.features.visitor.presentation.start.VisitorStartUiEvent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VisitorActivityDestinationResolverTest {

    @Test
    public void guidedEntryStillTargetsVisitorStartWrapper() {
        assertEquals(
                VisitorActivityDestinationResolver.Destination.VISITOR_START,
                VisitorActivityDestinationResolver.resolveEntryTarget(VisitorEntryPlan.Target.GUIDED_VISITOR_FLOW)
        );
    }

    @Test
    public void legacyEntryStillTargetsLegacyDialogWrapper() {
        assertEquals(
                VisitorActivityDestinationResolver.Destination.LEGACY_DIALOG,
                VisitorActivityDestinationResolver.resolveEntryTarget(VisitorEntryPlan.Target.LEGACY_DIALOG)
        );
    }

    @Test
    public void startEventTargetsContactWrapper() {
        assertEquals(
                VisitorActivityDestinationResolver.Destination.VISITOR_CONTACT,
                VisitorActivityDestinationResolver.resolveStartTarget(new VisitorStartUiEvent(VisitorStartUiEvent.Target.CONTACT_FLOW, "Ada"))
        );
    }

    @Test
    public void startEventTargetsMessageWrapper() {
        assertEquals(
                VisitorActivityDestinationResolver.Destination.VISITOR_LEAVE_MESSAGE,
                VisitorActivityDestinationResolver.resolveStartTarget(new VisitorStartUiEvent(VisitorStartUiEvent.Target.MESSAGE_FLOW, "Ada"))
        );
    }

    @Test
    public void startEventTargetsInformationWrapper() {
        assertEquals(
                VisitorActivityDestinationResolver.Destination.VISITOR_INFORMATION,
                VisitorActivityDestinationResolver.resolveStartTarget(new VisitorStartUiEvent(VisitorStartUiEvent.Target.INFORMATION_FLOW, "Ada"))
        );
    }

    @Test
    public void startEventKeepsLegacyBaseFallback() {
        assertEquals(
                VisitorActivityDestinationResolver.Destination.LEGACY_BASE,
                VisitorActivityDestinationResolver.resolveStartTarget(new VisitorStartUiEvent(VisitorStartUiEvent.Target.LEGACY_BASE, "Ada"))
        );
    }
}
