package com.mundosvirtuales.visitorassistant.visitorflow;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VisitorFlowPresenterTest {

    @Test
    public void startLoadsContactsAndShowsReadyState() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        contactsGateway.contacts = Arrays.asList(availableContact(), unavailableContact());
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();

        assertEquals(VisitorFlowState.Screen.LOADING, view.states.get(0).getScreen());
        assertEquals(VisitorFlowState.Screen.READY, view.last().getScreen());
        assertEquals(2, view.last().getContacts().size());
        assertEquals(1, speechPort.messages.size());
        assertEquals("Toque la persona o el área con la que desea comunicarse.", speechPort.messages.get(0));
    }

    @Test
    public void selectingAvailableContactShowsSuccess() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        notificationGateway.result = new VisitorDtos.NotificationResult("delivered_or_queued", "sent", "skipped", false, "Listo, avisamos a Ventas por Telegram.");
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact, "Ada");

        assertEquals(VisitorFlowState.Screen.SUBMITTING, view.states.get(2).getScreen());
        assertEquals(VisitorFlowState.Screen.SUCCESS, view.last().getScreen());
        assertEquals("Listo, avisamos a Ventas por Telegram.", view.last().getMessage());
        assertEquals(2, speechPort.messages.size());
        assertEquals("Listo, avisamos a Ventas por Telegram.", speechPort.messages.get(1));
    }

    @Test
    public void emptyContactsShowRecoverableState() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();

        assertEquals(VisitorFlowState.Screen.READY, view.last().getScreen());
        assertTrue(view.last().isRetryEnabled());
        assertEquals("No tengo contactos disponibles en este momento. Puede intentarlo nuevamente en unos segundos.", view.last().getMessage());
        assertEquals(1, speechPort.messages.size());
    }

    @Test
    public void selectingUnavailableContactNeverSubmits() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = unavailableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact, "Ada");

        assertEquals(VisitorFlowState.Screen.UNAVAILABLE, view.last().getScreen());
        assertFalse(notificationGateway.submitCalled);
        assertEquals(2, speechPort.messages.size());
        assertEquals("Este contacto no recibe notificaciones en este momento.", speechPort.messages.get(1));
    }

    @Test
    public void failedSubmissionCanRetryLastContact() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        notificationGateway.result = new VisitorDtos.NotificationResult("failed", "failed", "skipped", true, "Backend caído");
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact, "Ada");
        presenter.onRetry();

        assertTrue(notificationGateway.submitCalled);
        assertEquals(2, notificationGateway.submitCount);
        assertEquals(VisitorFlowState.Screen.FAILED, view.last().getScreen());
        assertTrue(view.last().isRetryEnabled());
    }

    @Test
    public void retryAfterInitialLoadErrorRecoversReadyState() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        contactsGateway.failuresBeforeSuccess = 1;
        contactsGateway.failureMessage = "No pude cargar los contactos. Revise la conexión con el backend.";
        contactsGateway.contacts = Arrays.asList(availableContact());
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();
        presenter.onRetry();

        assertEquals(VisitorFlowState.Screen.MAINTENANCE, view.states.get(1).getScreen());
        assertTrue(view.states.get(1).isRetryEnabled());
        assertEquals(VisitorFlowState.Screen.LOADING, view.states.get(2).getScreen());
        assertEquals(VisitorFlowState.Screen.READY, view.last().getScreen());
        assertEquals(1, view.last().getContacts().size());
        assertEquals(2, speechPort.messages.size());
        assertEquals("Recepción temporalmente fuera de servicio.", speechPort.messages.get(0));
        assertEquals("Toque la persona o el área con la que desea comunicarse.", speechPort.messages.get(1));
    }

    @Test
    public void startDoesNotExposeCachedContactsBeforeBackendConfirmation() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        contactsGateway.contacts = Arrays.asList(availableContact(), unavailableContact());
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();
        FakeContactCacheStore cacheStore = new FakeContactCacheStore();
        cacheStore.cachedContacts = Arrays.asList(unavailableContact());

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort, cacheStore);

        presenter.start();

        assertEquals(VisitorFlowState.Screen.LOADING, view.states.get(0).getScreen());
        assertFalse(view.states.get(0).isShowingCachedContacts());
        assertEquals(0, view.states.get(0).getContacts().size());
        assertEquals(VisitorFlowState.Screen.READY, view.last().getScreen());
        assertFalse(view.last().isShowingCachedContacts());
        assertEquals(2, view.last().getContacts().size());
        assertEquals(1, cacheStore.saveCount);
        assertEquals(2, cacheStore.savedContacts.size());
        assertEquals("ventas", cacheStore.savedContacts.get(0).getId());
        assertEquals(1, speechPort.messages.size());
    }

    @Test
    public void refreshFailureShowsMaintenanceInsteadOfCachedContacts() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        contactsGateway.failuresBeforeSuccess = 1;
        contactsGateway.failureMessage = "No pude cargar los contactos. Revise la conexión con el backend.";
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();
        FakeContactCacheStore cacheStore = new FakeContactCacheStore();
        cacheStore.cachedContacts = Arrays.asList(availableContact());

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort, cacheStore);

        presenter.start();

        assertEquals(VisitorFlowState.Screen.LOADING, view.states.get(0).getScreen());
        assertEquals(VisitorFlowState.Screen.MAINTENANCE, view.last().getScreen());
        assertFalse(view.last().isShowingCachedContacts());
        assertTrue(view.last().isRetryEnabled());
        assertEquals(0, view.last().getContacts().size());
        assertEquals("Recepción temporalmente fuera de servicio.", view.last().getMessage());
        assertEquals(1, speechPort.messages.size());
    }

    @Test
    public void initialTechnicalLoadErrorShowsMaintenanceState() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        contactsGateway.failuresBeforeSuccess = 1;
        contactsGateway.failureMessage = "El backend de visitas no está configurado en este robot.";
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();

        assertEquals(VisitorFlowState.Screen.MAINTENANCE, view.last().getScreen());
        assertEquals("Recepción temporalmente fuera de servicio.", view.last().getMessage());
        assertEquals(1, speechPort.messages.size());
        assertEquals("Recepción temporalmente fuera de servicio.", speechPort.messages.get(0));
    }

    @Test
    public void retryableSubmissionErrorSwitchesToMaintenance() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        notificationGateway.errorMessage = "No pude enviar la notificación. Revise la conexión con el backend.";
        notificationGateway.retryableError = true;
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact, "Ada");

        assertEquals(VisitorFlowState.Screen.MAINTENANCE, view.last().getScreen());
        assertEquals("Recepción temporalmente fuera de servicio.", view.last().getMessage());
    }

    private VisitorFlowPresenter buildPresenter(FakeView view,
                                                FakeContactsGateway contactsGateway,
                                                FakeNotificationGateway notificationGateway,
                                                FakeSpeechPort speechPort) {
        return buildPresenter(view, contactsGateway, notificationGateway, speechPort, new FakeContactCacheStore());
    }

    private VisitorFlowPresenter buildPresenter(FakeView view,
                                                FakeContactsGateway contactsGateway,
                                                FakeNotificationGateway notificationGateway,
                                                FakeSpeechPort speechPort,
                                                FakeContactCacheStore cacheStore) {
        return new VisitorFlowPresenter(
                view,
                contactsGateway,
                notificationGateway,
                cacheStore,
                speechPort,
                "Un momento, estoy preparando la lista de contactos…",
                "Actualizando la lista de contactos…",
                "Toque la persona o el área con la que desea comunicarse.",
                "Este contacto no recibe notificaciones en este momento.",
                "No pude completar el aviso. Revise la conexión e inténtelo nuevamente.",
                "No pude mostrar los contactos en este momento. Inténtelo nuevamente.",
                "Recepción temporalmente fuera de servicio.",
                "No tengo contactos disponibles en este momento. Puede intentarlo nuevamente en unos segundos.",
                "Listo, ya avisé a %1$s.",
                "Por favor, indique su nombre para continuar."
        );
    }

    @Test
    public void selectingAvailableContactRequiresVisitorNameBeforeSubmitting() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact, "  ");

        assertEquals(VisitorFlowState.Screen.READY, view.last().getScreen());
        assertFalse(notificationGateway.submitCalled);
        assertEquals("Por favor, indique su nombre para continuar.", view.last().getMessage());
        assertEquals("Por favor, indique su nombre para continuar.", speechPort.messages.get(1));
    }

    @Test
    public void selectingAvailableContactPassesVisitorNameToGateway() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeNotificationGateway notificationGateway = new FakeNotificationGateway();
        notificationGateway.result = new VisitorDtos.NotificationResult("accepted", "accepted", "skipped", false, "Listo, avisamos a Ventas por Telegram.");
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorFlowPresenter presenter = buildPresenter(view, contactsGateway, notificationGateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact, "  Ada Lovelace  ");

        assertEquals("Ada Lovelace", notificationGateway.visitorName);
    }

    private VisitorDtos.ContactSummary availableContact() {
        return new VisitorDtos.ContactSummary("ventas", "Ventas", true, false, true);
    }

    private VisitorDtos.ContactSummary unavailableContact() {
        return new VisitorDtos.ContactSummary("demo", "Demo", false, false, false);
    }

    private static class FakeView implements VisitorFlowPresenter.View {
        private final List<VisitorFlowState> states = new ArrayList<>();

        @Override
        public void render(VisitorFlowState state) {
            states.add(state);
        }

        private VisitorFlowState last() {
            return states.get(states.size() - 1);
        }
    }

    private static class FakeContactCacheStore implements VisitorContactCacheStore {
        private List<VisitorDtos.ContactSummary> cachedContacts = new ArrayList<>();
        private List<VisitorDtos.ContactSummary> savedContacts = new ArrayList<>();
        private int saveCount;

        @Override
        public List<VisitorDtos.ContactSummary> getCachedContacts() {
            return cachedContacts;
        }

        @Override
        public void saveContacts(List<VisitorDtos.ContactSummary> contacts) {
            saveCount++;
            savedContacts = new ArrayList<>(contacts);
        }
    }

    private static class FakeContactsGateway implements ContactCatalogGateway {
        private List<VisitorDtos.ContactSummary> contacts = new ArrayList<>();
        private int failuresBeforeSuccess;
        private String failureMessage;

        @Override
        public void fetchContacts(Callback callback) {
            if (failuresBeforeSuccess > 0) {
                failuresBeforeSuccess--;
                callback.onError(failureMessage);
                return;
            }
            callback.onSuccess(contacts);
        }
    }

    private static class FakeNotificationGateway implements NotificationDispatchGateway {
        private VisitorDtos.NotificationResult result;
        private boolean submitCalled;
        private int submitCount;
        private String visitorName;
        private String errorMessage;
        private boolean retryableError;

        @Override
        public void submitNotification(VisitorDtos.ContactSummary contact, String visitorName, Callback callback) {
            submitCalled = true;
            submitCount++;
            this.visitorName = visitorName;
            if (errorMessage != null) {
                callback.onError(errorMessage, retryableError);
                return;
            }
            callback.onSuccess(result);
        }
    }

    private static class FakeSpeechPort implements RobotSpeechPort {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void speak(String message) {
            messages.add(message);
        }
    }
}
