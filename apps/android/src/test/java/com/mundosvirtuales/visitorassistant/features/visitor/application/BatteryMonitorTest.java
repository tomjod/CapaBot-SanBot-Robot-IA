package com.mundosvirtuales.visitorassistant.features.visitor.application;

import com.mundosvirtuales.visitorassistant.features.visitor.domain.BatteryStatusSnapshot;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BatteryMonitorTest {

    private final BatteryMonitor monitor = new BatteryMonitor();

    @Test
    public void isChargingRecognizesLineAndPileStatuses() {
        assertTrue(monitor.isCharging(new BatteryStatusSnapshot(40, 5), 5, 6));
        assertTrue(monitor.isCharging(new BatteryStatusSnapshot(40, 6), 5, 6));
        assertFalse(monitor.isCharging(new BatteryStatusSnapshot(40, 3), 5, 6));
    }

    @Test
    public void shouldStartChargingOnlyWhenAllowedAndBelowThreshold() {
        assertTrue(monitor.shouldStartCharging(true, new BatteryStatusSnapshot(15, 3), 20, 5, 6));
        assertFalse(monitor.shouldStartCharging(false, new BatteryStatusSnapshot(15, 3), 20, 5, 6));
        assertFalse(monitor.shouldStartCharging(true, new BatteryStatusSnapshot(15, 5), 20, 5, 6));
        assertFalse(monitor.shouldStartCharging(true, new BatteryStatusSnapshot(35, 3), 20, 5, 6));
    }
}
