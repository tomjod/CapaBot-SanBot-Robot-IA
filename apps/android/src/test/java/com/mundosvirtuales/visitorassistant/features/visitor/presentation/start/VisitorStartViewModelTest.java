package com.mundosvirtuales.visitorassistant.features.visitor.presentation.start;

import com.mundosvirtuales.visitorassistant.features.visitor.data.VisitorAvailabilityGateway;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorStartNavigation;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VisitorStartViewModelTest {

    @Test
    public void startPublishesCheckingThenAvailableState() {
        FakeAvailabilityGateway gateway = new FakeAvailabilityGateway();
        gateway.available = true;
        VisitorStartViewModel viewModel = new VisitorStartViewModel(gateway, "Checking", "Maintenance", "Ada");
        List<VisitorStartUiState> states = new ArrayList<>();
        List<VisitorStartUiEvent> events = new ArrayList<>();

        viewModel.observe(states::add, events::add);
        viewModel.start();

        assertEquals(3, states.size());
        assertEquals(VisitorStartUiState.Screen.CHECKING, states.get(0).getScreen());
        assertEquals(VisitorStartUiState.Screen.CHECKING, states.get(1).getScreen());
        assertEquals(VisitorStartUiState.Screen.AVAILABLE, states.get(2).getScreen());
        assertTrue(events.isEmpty());
    }

    @Test
    public void retryPublishesMaintenanceWhenAvailabilityFails() {
        FakeAvailabilityGateway gateway = new FakeAvailabilityGateway();
        gateway.available = false;
        VisitorStartViewModel viewModel = new VisitorStartViewModel(gateway, "Checking", "Maintenance", "Ada");
        List<VisitorStartUiState> states = new ArrayList<>();

        viewModel.observe(states::add, event -> { });
        viewModel.onRetry();

        VisitorStartUiState lastState = states.get(states.size() - 1);
        assertEquals(VisitorStartUiState.Screen.MAINTENANCE, lastState.getScreen());
        assertEquals("Maintenance", lastState.getMessage());
        assertTrue(lastState.isRetryEnabled());
    }

    @Test
    public void visitReasonSelectionEmitsOnlyEnabledTargets() {
        FakeAvailabilityGateway gateway = new FakeAvailabilityGateway();
        gateway.available = true;
        VisitorStartViewModel viewModel = new VisitorStartViewModel(gateway, "Checking", "Maintenance", "Ada");
        List<VisitorStartUiEvent> events = new ArrayList<>();

        viewModel.observe(state -> { }, events::add);
        viewModel.start();
        viewModel.onVisitReasonSelected(VisitorStartNavigation.VisitReason.TALK_TO_PERSON);
        viewModel.onVisitReasonSelected(VisitorStartNavigation.VisitReason.REQUEST_INFORMATION);

        assertEquals(2, events.size());
        assertEquals(VisitorStartUiEvent.Target.CONTACT_FLOW, events.get(0).getTarget());
        assertEquals("Ada", events.get(0).getVisitorName());
        assertEquals(VisitorStartUiEvent.Target.INFORMATION_FLOW, events.get(1).getTarget());
    }

    @Test
    public void visitReasonSelectionStaysBlockedDuringMaintenanceForReceptionFlows() {
        FakeAvailabilityGateway gateway = new FakeAvailabilityGateway();
        gateway.available = false;
        VisitorStartViewModel viewModel = new VisitorStartViewModel(gateway, "Checking", "Maintenance", "Ada");
        List<VisitorStartUiEvent> events = new ArrayList<>();

        viewModel.observe(state -> { }, events::add);
        viewModel.start();
        viewModel.onVisitReasonSelected(VisitorStartNavigation.VisitReason.TALK_TO_PERSON);
        viewModel.onVisitReasonSelected(VisitorStartNavigation.VisitReason.LEGACY_BASE);

        assertEquals(1, events.size());
        assertEquals(VisitorStartUiEvent.Target.LEGACY_BASE, events.get(0).getTarget());
    }

    private static class FakeAvailabilityGateway implements VisitorAvailabilityGateway {
        private boolean available;

        @Override
        public void checkAvailability(Callback callback) {
            if (available) {
                callback.onAvailable();
                return;
            }
            callback.onUnavailable("offline");
        }
    }
}
