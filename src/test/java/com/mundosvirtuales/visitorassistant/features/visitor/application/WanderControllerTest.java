package com.mundosvirtuales.visitorassistant.features.visitor.application;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WanderControllerTest {

    private final WanderController controller = new WanderController();

    @Test
    public void shouldEnableWanderOnlyWhenIdleAndAllowed() {
        assertTrue(controller.shouldEnableWander(false, true));
        assertFalse(controller.shouldEnableWander(true, true));
        assertFalse(controller.shouldEnableWander(false, false));
    }

    @Test
    public void shouldStopForInteractionOnlyWhenRobotIsAvailable() {
        assertTrue(controller.shouldStopForInteraction(false));
        assertFalse(controller.shouldStopForInteraction(true));
    }
}
