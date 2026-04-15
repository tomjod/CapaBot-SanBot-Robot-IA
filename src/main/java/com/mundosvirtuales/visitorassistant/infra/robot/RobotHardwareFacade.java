package com.mundosvirtuales.visitorassistant.infra.robot;

import com.mundosvirtuales.visitorassistant.features.visitor.domain.BatteryStatusSnapshot;

public interface RobotHardwareFacade {
    void setWanderEnabled(boolean enabled);

    BatteryStatusSnapshot readBatteryStatus();
}
