package com.mundosvirtuales.visitorassistant.features.visitor.presentation.contact;

import com.mundosvirtuales.visitorassistant.visitorflow.VisitorContactCacheStore;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorDtos;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorFlowPresenter;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorFlowState;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class VisitorContactViewModelTest {

    @Test
    public void onContactTappedPromptsForVisitorNameWhenCurrentNameIsMissing() {
        SpyVisitorFlowPresenter presenter = new SpyVisitorFlowPresenter();
        VisitorContactViewModel viewModel = new VisitorContactViewModel(presenter, strings());
        List<VisitorContactUiEvent> events = new ArrayList<>();

        viewModel.observe(state -> { }, events::add);
        VisitorDtos.ContactSummary contact = contact("ventas", "Ventas", true);
        viewModel.onContactTapped(contact, "   ");

        assertEquals(1, events.size());
        assertEquals(VisitorContactUiEvent.Type.PROMPT_VISITOR_NAME, events.get(0).getType());
        assertSame(contact, events.get(0).getContact());
        assertFalse(presenter.contactSelectedCalled);
    }

    @Test
    public void onVisitorNameProvidedRejectsBlankNames() {
        SpyVisitorFlowPresenter presenter = new SpyVisitorFlowPresenter();
        VisitorContactViewModel viewModel = new VisitorContactViewModel(presenter, strings());
        List<VisitorContactUiEvent> events = new ArrayList<>();
        VisitorDtos.ContactSummary contact = contact("ventas", "Ventas", true);

        viewModel.observe(state -> { }, events::add);
        boolean submitted = viewModel.onVisitorNameProvided(contact, "   ");

        assertFalse(submitted);
        assertEquals(1, events.size());
        assertEquals(VisitorContactUiEvent.Type.INVALID_VISITOR_NAME, events.get(0).getType());
        assertFalse(presenter.contactSelectedCalled);
    }

    @Test
    public void onVisitorNameProvidedNormalizesAndSubmitsName() {
        SpyVisitorFlowPresenter presenter = new SpyVisitorFlowPresenter();
        VisitorContactViewModel viewModel = new VisitorContactViewModel(presenter, strings());
        VisitorDtos.ContactSummary contact = contact("ventas", "Ventas", true);

        boolean submitted = viewModel.onVisitorNameProvided(contact, "  Ada Lovelace  ");

        assertTrue(submitted);
        assertTrue(presenter.contactSelectedCalled);
        assertSame(contact, presenter.lastContact);
        assertEquals("Ada Lovelace", presenter.lastVisitorName);
    }

    @Test
    public void renderMapsSubmittingAndSuccessStates() {
        SpyVisitorFlowPresenter presenter = new SpyVisitorFlowPresenter();
        VisitorContactViewModel viewModel = new VisitorContactViewModel(presenter, strings());
        List<VisitorContactUiState> states = new ArrayList<>();
        VisitorDtos.ContactSummary contact = contact("ventas", "Ventas", true);
        List<VisitorDtos.ContactSummary> contacts = Collections.singletonList(contact);

        viewModel.observe(states::add, event -> { });
        viewModel.render(VisitorFlowState.ready(contacts, "ready", false, true));
        viewModel.render(VisitorFlowState.submitting(contacts, "ignored", contact.getId()));
        viewModel.render(VisitorFlowState.success(contacts, "Listo", contact.getId()));

        VisitorContactUiState submittingState = states.get(1);
        assertEquals("Avisando a Ventas…", submittingState.getStatusMessage());
        assertTrue(submittingState.isStatusVisible());
        assertTrue(submittingState.isProgressVisible());
        assertEquals("Usando cache por reintento", states.get(0).getCacheHint());

        VisitorContactUiState successState = states.get(2);
        assertTrue(successState.isSuccess());
        assertEquals("Listo", successState.getSuccessMessage());
        assertFalse(successState.isProgressVisible());
        assertFalse(successState.isMaintenanceVisible());
    }

    @Test
    public void renderKeepsMaintenanceRetryVisibleAndContactsDisabled() {
        VisitorContactViewModel viewModel = new VisitorContactViewModel(new SpyVisitorFlowPresenter(), strings());
        List<VisitorContactUiState> states = new ArrayList<>();

        viewModel.observe(states::add, event -> { });
        viewModel.render(VisitorFlowState.maintenance("Fuera de servicio", true));

        VisitorContactUiState state = states.get(0);
        assertTrue(state.isMaintenanceVisible());
        assertEquals("Fuera de servicio", state.getMaintenanceMessage());
        assertTrue(state.isRetryVisible());
        assertFalse(state.isContactsEnabled());
    }

    private VisitorContactViewModel.Strings strings() {
        return new VisitorContactViewModel.Strings(
                "Puede elegir un contacto",
                "Usando cache durante la carga",
                "Usando cache por reintento",
                "Avisando a %s…"
        );
    }

    private VisitorDtos.ContactSummary contact(String id, String displayName, boolean available) {
        return new VisitorDtos.ContactSummary(id, displayName, available, false, available);
    }

    private static class SpyVisitorFlowPresenter extends VisitorFlowPresenter {
        private boolean contactSelectedCalled;
        private VisitorDtos.ContactSummary lastContact;
        private String lastVisitorName;

        SpyVisitorFlowPresenter() {
            super(
                    state -> { },
                    callback -> callback.onSuccess(Collections.<VisitorDtos.ContactSummary>emptyList()),
                    (contact, visitorName, callback) -> callback.onSuccess(new VisitorDtos.NotificationResult("accepted", "accepted", "skipped", false, "ok")),
                    new VisitorContactCacheStore() {
                        @Override
                        public void saveContacts(List<VisitorDtos.ContactSummary> contacts) {
                        }

                        @Override
                        public List<VisitorDtos.ContactSummary> getCachedContacts() {
                            return Collections.emptyList();
                        }
                    },
                    message -> { },
                    "loading",
                    "cached loading",
                    "ready",
                    "unavailable",
                    "failed",
                    "load failed",
                    "maintenance",
                    "empty",
                    "%s",
                    "name required"
            );
        }

        @Override
        public void onContactSelected(VisitorDtos.ContactSummary contact, String visitorName) {
            contactSelectedCalled = true;
            lastContact = contact;
            lastVisitorName = visitorName;
        }
    }
}
