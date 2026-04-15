package com.mundosvirtuales.visitorassistant.features.visitor.application;

import com.mundosvirtuales.visitorassistant.features.visitor.domain.BatteryStatusSnapshot;

public class BatteryMonitor {

    public boolean isCharging(BatteryStatusSnapshot snapshot, int chargeLineStatus, int chargePileStatus) {
        return snapshot.getBatteryStatus() == chargeLineStatus || snapshot.getBatteryStatus() == chargePileStatus;
    }

    public boolean shouldStartCharging(boolean autoChargeAllowed,
                                       BatteryStatusSnapshot snapshot,
                                       int batteryLowThreshold,
                                       int chargeLineStatus,
                                       int chargePileStatus) {
        return autoChargeAllowed
                && !isCharging(snapshot, chargeLineStatus, chargePileStatus)
                && snapshot.getBatteryValue() <= batteryLowThreshold;
    }
}
