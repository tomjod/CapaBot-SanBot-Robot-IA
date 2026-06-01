package com.mundosvirtuales.visitorassistant.infra.robot;

import com.mundosvirtuales.visitorassistant.features.visitor.domain.BatteryStatusSnapshot;
import com.sanbot.opensdk.function.unit.ModularMotionManager;
import com.sanbot.opensdk.function.unit.SystemManager;

public class SanbotRobotHardwareFacade implements RobotHardwareFacade {

    private final ModularMotionManager modularMotionManager;
    private final SystemManager systemManager;

    public SanbotRobotHardwareFacade(ModularMotionManager modularMotionManager, SystemManager systemManager) {
        this.modularMotionManager = modularMotionManager;
        this.systemManager = systemManager;
    }

    @Override
    public void setWanderEnabled(boolean enabled) {
        modularMotionManager.switchWander(enabled);
    }

    @Override
    public BatteryStatusSnapshot readBatteryStatus() {
        return new BatteryStatusSnapshot(systemManager.getBatteryValue(), systemManager.getBatteryStatus());
    }
}
