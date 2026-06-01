package com.mundosvirtuales.visitorassistant.features.visitor.application;

import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorEntryPlan;
import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorEntryRequest;
import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorEntryScript;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class VisitorEntryCoordinatorTest {

    private final VisitorEntryCoordinator coordinator = new VisitorEntryCoordinator();

    @Test
    public void createPlanReturnsBlockedWhenRobotIsBusy() {
        VisitorEntryPlan plan = coordinator.createPlan(
                new VisitorEntryRequest(true, true, "Ada", true, "https://api.example", 9, 0.1d),
                script()
        );

        assertFalse(plan.shouldStart());
        assertEquals(VisitorEntryPlan.Target.LEGACY_DIALOG, plan.getTarget());
        assertTrue(plan.getSpokenMessages().isEmpty());
    }

    @Test
    public void createPlanBuildsGuidedFlowMessagesForNamedVisitor() {
        VisitorEntryPlan plan = coordinator.createPlan(
                new VisitorEntryRequest(false, true, "  Ada Lovelace  ", true, "https://api.example", 9, 0.1d),
                script()
        );

        assertTrue(plan.shouldStart());
        assertEquals("Ada Lovelace", plan.getNormalizedVisitorName());
        assertEquals(VisitorEntryPlan.Target.GUIDED_VISITOR_FLOW, plan.getTarget());
        assertEquals(3, plan.getSpokenMessages().size());
        assertEquals("Hola, Ada Lovelace", plan.getSpokenMessages().get(0));
        assertEquals("Buen día", plan.getSpokenMessages().get(1));
        assertEquals("Ada Lovelace, te acompaño con la visita guiada.", plan.getSpokenMessages().get(2));
    }

    @Test
    public void createPlanFallsBackToLegacyDialogAndGenericGreeting() {
        VisitorEntryPlan plan = coordinator.createPlan(
                new VisitorEntryRequest(false, true, "   ", true, "   ", 20, 0.9d),
                script()
        );

        assertTrue(plan.shouldStart());
        assertNull(plan.getNormalizedVisitorName());
        assertEquals(VisitorEntryPlan.Target.LEGACY_DIALOG, plan.getTarget());
        assertEquals(1, plan.getSpokenMessages().size());
        assertEquals("Hola", plan.getSpokenMessages().get(0));
    }

    private VisitorEntryScript script() {
        return new VisitorEntryScript(
                "Hola",
                "Te acompaño con la visita guiada.",
                "%s, te acompaño con la visita guiada.",
                "Buenas madrugadas",
                "Buen día",
                "Buenas tardes",
                "Buenas noches"
        );
    }
}
