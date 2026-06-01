package com.mundosvirtuales.visitorassistant.features.visitor.application;

public class WanderController {

    public boolean shouldEnableWander(boolean busy, boolean wanderAllowed) {
        return !busy && wanderAllowed;
    }

    public boolean shouldStopForInteraction(boolean busy) {
        return !busy;
    }
}
