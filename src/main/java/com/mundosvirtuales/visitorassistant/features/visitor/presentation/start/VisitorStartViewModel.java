package com.mundosvirtuales.visitorassistant.features.visitor.presentation.start;

import com.mundosvirtuales.visitorassistant.core.mvvm.SimpleEventDispatcher;
import com.mundosvirtuales.visitorassistant.core.mvvm.SimpleStateStore;
import com.mundosvirtuales.visitorassistant.core.mvvm.UiEventListener;
import com.mundosvirtuales.visitorassistant.core.mvvm.UiStateListener;
import com.mundosvirtuales.visitorassistant.features.visitor.data.VisitorAvailabilityGateway;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorFlowLogger;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorStartNavigation;

public class VisitorStartViewModel {

    private final VisitorAvailabilityGateway availabilityGateway;
    private final String checkingMessage;
    private final String maintenanceMessage;
    private final String visitorName;
    private final SimpleStateStore<VisitorStartUiState> stateStore = new SimpleStateStore<>();
    private final SimpleEventDispatcher<VisitorStartUiEvent> eventDispatcher = new SimpleEventDispatcher<>();

    public VisitorStartViewModel(VisitorAvailabilityGateway availabilityGateway,
                                 String checkingMessage,
                                 String maintenanceMessage,
                                 String visitorName) {
        this.availabilityGateway = availabilityGateway;
        this.checkingMessage = checkingMessage;
        this.maintenanceMessage = maintenanceMessage;
        this.visitorName = visitorName;
        stateStore.setState(VisitorStartUiState.checking(checkingMessage));
    }

    public void observe(UiStateListener<VisitorStartUiState> stateListener,
                        UiEventListener<VisitorStartUiEvent> eventListener) {
        stateStore.observe(stateListener);
        eventDispatcher.observe(eventListener);
    }

    public void start() {
        checkAvailability();
    }

    public void onRetry() {
        checkAvailability();
    }

    public void onVisitReasonSelected(VisitorStartNavigation.VisitReason visitReason) {
        VisitorStartUiState currentState = stateStore.getState();
        if (currentState == null) {
            VisitorFlowLogger.warn("visitor.start.route.blocked", "reason=" + visitReason + ", state=null");
            return;
        }

        VisitorStartNavigation.Target target = VisitorStartNavigation.resolveTarget(visitReason);
        if ((target == VisitorStartNavigation.Target.CONTACT_FLOW || target == VisitorStartNavigation.Target.MESSAGE_FLOW)
                && !currentState.isReceptionAccessEnabled()) {
            VisitorFlowLogger.warn("visitor.start.route.blocked", "reason=" + visitReason + ", receptionAccess=false");
            return;
        }
        if (target == VisitorStartNavigation.Target.INFORMATION_FLOW && !currentState.isInformationAccessEnabled()) {
            VisitorFlowLogger.warn("visitor.start.route.blocked", "reason=" + visitReason + ", informationAccess=false");
            return;
        }
        if (target == VisitorStartNavigation.Target.LEGACY_BASE && !currentState.isLegacyAccessEnabled()) {
            VisitorFlowLogger.warn("visitor.start.route.blocked", "reason=" + visitReason + ", legacyAccess=false");
            return;
        }

        if (target == VisitorStartNavigation.Target.CONTACT_FLOW) {
            eventDispatcher.emit(new VisitorStartUiEvent(VisitorStartUiEvent.Target.CONTACT_FLOW, visitorName));
            return;
        }
        if (target == VisitorStartNavigation.Target.MESSAGE_FLOW) {
            eventDispatcher.emit(new VisitorStartUiEvent(VisitorStartUiEvent.Target.MESSAGE_FLOW, visitorName));
            return;
        }
        if (target == VisitorStartNavigation.Target.INFORMATION_FLOW) {
            eventDispatcher.emit(new VisitorStartUiEvent(VisitorStartUiEvent.Target.INFORMATION_FLOW, visitorName));
            return;
        }
        eventDispatcher.emit(new VisitorStartUiEvent(VisitorStartUiEvent.Target.LEGACY_BASE, visitorName));
    }

    private void checkAvailability() {
        VisitorFlowLogger.info("visitor.start.checkAvailability", "render=checking");
        stateStore.setState(VisitorStartUiState.checking(checkingMessage));
        availabilityGateway.checkAvailability(new VisitorAvailabilityGateway.Callback() {
            @Override
            public void onAvailable() {
                VisitorFlowLogger.info("visitor.start.available", "visitor flow available");
                stateStore.setState(VisitorStartUiState.available());
            }

            @Override
            public void onUnavailable(String message) {
                VisitorFlowLogger.warn("visitor.start.maintenance", "reason=" + message);
                stateStore.setState(VisitorStartUiState.maintenance(maintenanceMessage));
            }
        });
    }
}
