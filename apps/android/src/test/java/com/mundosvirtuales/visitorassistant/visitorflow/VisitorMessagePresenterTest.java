package com.mundosvirtuales.visitorassistant.visitorflow;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VisitorMessagePresenterTest {

    @Test
    public void selectingAvailableContactAndValidDraftEnablesSend() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);

        VisitorMessagePresenter presenter = buildPresenter(view, contactsGateway, new FakeMessageGateway(), new FakeSpeechPort());

        presenter.start();
        presenter.onContactSelected(contact);
        presenter.onMessageChanged("Necesito que me contacten al finalizar la reunión.");

        assertEquals(VisitorMessageFlowState.Screen.READY, view.last().getScreen());
        assertTrue(view.last().isSendEnabled());
        assertEquals(contact.getId(), view.last().getSelectedContactId());
    }

    @Test
    public void submitWithoutMessageKeepsReadyStateAndDoesNotDispatch() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeMessageGateway gateway = new FakeMessageGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorMessagePresenter presenter = buildPresenter(view, contactsGateway, gateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact);
        presenter.onSubmit("Ada");

        assertEquals(VisitorMessageFlowState.Screen.READY, view.last().getScreen());
        assertFalse(gateway.submitCalled);
        assertEquals("Escriba un mensaje breve antes de enviarlo.", view.last().getMessage());
        assertEquals("Escriba un mensaje breve antes de enviarlo.", speechPort.messages.get(1));
    }

    @Test
    public void successfulSubmissionShowsSuccessState() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeMessageGateway gateway = new FakeMessageGateway();
        gateway.result = new VisitorDtos.NotificationResult("delivered_or_queued", "sent", "skipped", false, "Listo, dejamos su mensaje para Ventas por Telegram.");
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorMessagePresenter presenter = buildPresenter(view, contactsGateway, gateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact);
        presenter.onMessageChanged("Necesito una devolución.");
        presenter.onSubmit("Ada");

        assertTrue(gateway.submitCalled);
        assertEquals("Necesito una devolución.", gateway.message);
        assertEquals(VisitorMessageFlowState.Screen.SUCCESS, view.last().getScreen());
        assertEquals("Listo, dejamos su mensaje para Ventas por Telegram.", view.last().getMessage());
        assertEquals("Listo, dejamos su mensaje para Ventas por Telegram.", speechPort.messages.get(1));
    }

    @Test
    public void refreshFailureShowsMaintenanceState() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        contactsGateway.failuresBeforeSuccess = 1;
        contactsGateway.failureMessage = "No pude cargar los contactos. Revise la conexión con el backend.";
        FakeContactCacheStore cacheStore = new FakeContactCacheStore();
        cacheStore.cachedContacts = Arrays.asList(availableContact());

        VisitorMessagePresenter presenter = new VisitorMessagePresenter(
                view,
                contactsGateway,
                new FakeMessageGateway(),
                cacheStore,
                new FakeSpeechPort(),
                "Cargando",
                "Actualizando",
                "Seleccione un contacto",
                "Contacto elegido: %s",
                "No disponible",
                "Falló",
                "Recepción temporalmente fuera de servicio.",
                "Sin contactos",
                "Seleccione contacto",
                "Nombre requerido",
                "Escriba mensaje",
                "Muy largo",
                "Listo, dejé su mensaje para %s."
        );

        presenter.start();

        assertEquals(VisitorMessageFlowState.Screen.MAINTENANCE, view.last().getScreen());
        assertFalse(view.last().isShowingCachedContacts());
        assertTrue(view.last().isRetryEnabled());
        assertEquals(0, view.last().getContacts().size());
    }

    @Test
    public void retryableDispatchErrorShowsMaintenanceState() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeMessageGateway gateway = new FakeMessageGateway();
        gateway.errorMessage = "No pude enviar el mensaje. Revise la conexión con el backend.";
        gateway.retryableError = true;
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorMessagePresenter presenter = buildPresenter(view, contactsGateway, gateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact);
        presenter.onMessageChanged("Necesito una devolución.");
        presenter.onSubmit("Ada");

        assertEquals(VisitorMessageFlowState.Screen.MAINTENANCE, view.last().getScreen());
        assertEquals("Recepción temporalmente fuera de servicio.", view.last().getMessage());
    }

    private VisitorMessagePresenter buildPresenter(FakeView view,
                                                   FakeContactsGateway contactsGateway,
                                                   FakeMessageGateway gateway,
                                                   FakeSpeechPort speechPort) {
        return new VisitorMessagePresenter(
                view,
                contactsGateway,
                gateway,
                new FakeContactCacheStore(),
                speechPort,
                "Cargando",
                "Actualizando",
                "Seleccione un contacto y escriba un mensaje breve.",
                "Contacto elegido: %s. Ahora puede escribir el mensaje.",
                "Este contacto no puede recibir mensajes en este momento.",
                "No pude enviar el mensaje. Revise la conexión e inténtelo nuevamente.",
                "Recepción temporalmente fuera de servicio.",
                "No tengo contactos disponibles para mensajes en este momento.",
                "Primero elija a quién desea dejarle el mensaje.",
                "Por favor, indique su nombre para continuar.",
                "Escriba un mensaje breve antes de enviarlo.",
                "El mensaje debe tener hasta 280 caracteres.",
                "Listo, dejé su mensaje para %s."
        );
    }

    @Test
    public void submitWithoutVisitorNameKeepsReadyStateAndDoesNotDispatch() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeMessageGateway gateway = new FakeMessageGateway();
        FakeSpeechPort speechPort = new FakeSpeechPort();

        VisitorMessagePresenter presenter = buildPresenter(view, contactsGateway, gateway, speechPort);

        presenter.start();
        presenter.onContactSelected(contact);
        presenter.onMessageChanged("Necesito una devolución.");
        presenter.onSubmit(" ");

        assertEquals(VisitorMessageFlowState.Screen.READY, view.last().getScreen());
        assertFalse(gateway.submitCalled);
        assertEquals("Por favor, indique su nombre para continuar.", view.last().getMessage());
        assertEquals("Por favor, indique su nombre para continuar.", speechPort.messages.get(1));
    }

    @Test
    public void successfulSubmissionPassesVisitorNameToGateway() {
        FakeView view = new FakeView();
        FakeContactsGateway contactsGateway = new FakeContactsGateway();
        VisitorDtos.ContactSummary contact = availableContact();
        contactsGateway.contacts = Arrays.asList(contact);
        FakeMessageGateway gateway = new FakeMessageGateway();
        gateway.result = new VisitorDtos.NotificationResult("accepted", "accepted", "skipped", false, "Listo, dejamos su mensaje para Ventas por Telegram.");

        VisitorMessagePresenter presenter = buildPresenter(view, contactsGateway, gateway, new FakeSpeechPort());

        presenter.start();
        presenter.onContactSelected(contact);
        presenter.onMessageChanged("Necesito una devolución.");
        presenter.onSubmit("  Ada Lovelace ");

        assertEquals("Ada Lovelace", gateway.visitorName);
    }

    private VisitorDtos.ContactSummary availableContact() {
        return new VisitorDtos.ContactSummary("ventas", "Ventas", true, false, true);
    }

    private static class FakeView implements VisitorMessagePresenter.View {
        private final List<VisitorMessageFlowState> states = new ArrayList<>();

        @Override
        public void render(VisitorMessageFlowState state) {
            states.add(state);
        }

        private VisitorMessageFlowState last() {
            return states.get(states.size() - 1);
        }
    }

    private static class FakeContactCacheStore implements VisitorContactCacheStore {
        private List<VisitorDtos.ContactSummary> cachedContacts = new ArrayList<>();

        @Override
        public List<VisitorDtos.ContactSummary> getCachedContacts() {
            return cachedContacts;
        }

        @Override
        public void saveContacts(List<VisitorDtos.ContactSummary> contacts) {
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

    private static class FakeMessageGateway implements MessageDispatchGateway {
        private VisitorDtos.NotificationResult result;
        private boolean submitCalled;
        private String visitorName;
        private String message;
        private String errorMessage;
        private boolean retryableError;

        @Override
        public void submitMessageNotification(VisitorDtos.ContactSummary contact, String visitorName, String message, NotificationDispatchGateway.Callback callback) {
            this.submitCalled = true;
            this.visitorName = visitorName;
            this.message = message;
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
