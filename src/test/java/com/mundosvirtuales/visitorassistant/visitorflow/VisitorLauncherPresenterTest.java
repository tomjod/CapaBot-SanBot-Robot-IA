package com.mundosvirtuales.visitorassistant.visitorflow;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VisitorLauncherPresenterTest {

    @Test
    public void startEnablesVisitorAccessAfterSuccessfulAvailabilityCheck() {
        FakeView view = new FakeView();
        FakeContactsGateway gateway = new FakeContactsGateway();
        VisitorLauncherPresenter presenter = new VisitorLauncherPresenter(
                view,
                gateway,
                "Verificando",
                "Recepción temporalmente fuera de servicio."
        );

        presenter.start();

        assertEquals(VisitorLauncherState.Screen.CHECKING, view.states.get(0).getScreen());
        assertEquals(VisitorLauncherState.Screen.AVAILABLE, view.last().getScreen());
        assertTrue(view.last().isVisitorAccessEnabled());
        assertTrue(view.last().isReceptionAccessEnabled());
        assertTrue(view.last().isInformationAccessEnabled());
    }

    @Test
    public void startShowsMaintenanceWhenAvailabilityCannotBeConfirmed() {
        FakeView view = new FakeView();
        FakeContactsGateway gateway = new FakeContactsGateway();
        gateway.errorMessage = "No pude cargar los contactos. Revise la conexión con el backend.";
        VisitorLauncherPresenter presenter = new VisitorLauncherPresenter(
                view,
                gateway,
                "Verificando",
                "Recepción temporalmente fuera de servicio."
        );

        presenter.start();

        assertEquals(VisitorLauncherState.Screen.MAINTENANCE, view.last().getScreen());
        assertEquals("Recepción temporalmente fuera de servicio.", view.last().getMessage());
        assertFalse(view.last().isVisitorAccessEnabled());
        assertFalse(view.last().isReceptionAccessEnabled());
        assertTrue(view.last().isInformationAccessEnabled());
        assertTrue(view.last().isLegacyAccessEnabled());
        assertTrue(view.last().isRetryEnabled());
    }

    private static class FakeView implements VisitorLauncherPresenter.View {
        private final List<VisitorLauncherState> states = new ArrayList<>();

        @Override
        public void render(VisitorLauncherState state) {
            states.add(state);
        }

        private VisitorLauncherState last() {
            return states.get(states.size() - 1);
        }
    }

    private static class FakeContactsGateway implements ContactCatalogGateway {
        private String errorMessage;

        @Override
        public void fetchContacts(Callback callback) {
            if (errorMessage != null) {
                callback.onError(errorMessage);
                return;
            }
            callback.onSuccess(new ArrayList<VisitorDtos.ContactSummary>());
        }
    }
}
