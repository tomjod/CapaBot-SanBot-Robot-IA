package com.mundosvirtuales.visitorassistant.visitorflow;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VisitorStartNavigationTest {

    @Test
    public void automaticGreetingRoutesToVisitorStartMenu() {
        assertEquals(
                VisitorStartNavigation.Target.START_MENU,
                VisitorStartNavigation.resolveAutomaticGreetingTarget()
        );
    }

    @Test
    public void talkToPersonRoutesToContactFlow() {
        assertEquals(
                VisitorStartNavigation.Target.CONTACT_FLOW,
                VisitorStartNavigation.resolveTarget(VisitorStartNavigation.VisitReason.TALK_TO_PERSON)
        );
    }

    @Test
    public void messageAndInformationRouteToDedicatedMvpFlows() {
        assertEquals(
                VisitorStartNavigation.Target.MESSAGE_FLOW,
                VisitorStartNavigation.resolveTarget(VisitorStartNavigation.VisitReason.LEAVE_MESSAGE)
        );
        assertEquals(
                VisitorStartNavigation.Target.INFORMATION_FLOW,
                VisitorStartNavigation.resolveTarget(VisitorStartNavigation.VisitReason.REQUEST_INFORMATION)
        );
    }

    @Test
    public void legacyBaseRemainsReachable() {
        assertEquals(
                VisitorStartNavigation.Target.LEGACY_BASE,
                VisitorStartNavigation.resolveTarget(VisitorStartNavigation.VisitReason.LEGACY_BASE)
        );
    }
}
