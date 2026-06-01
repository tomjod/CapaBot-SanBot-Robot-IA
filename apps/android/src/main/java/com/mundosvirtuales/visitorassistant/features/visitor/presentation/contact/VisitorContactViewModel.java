package com.mundosvirtuales.visitorassistant.features.visitor.presentation.contact;

import com.mundosvirtuales.visitorassistant.core.mvvm.SimpleEventDispatcher;
import com.mundosvirtuales.visitorassistant.core.mvvm.SimpleStateStore;
import com.mundosvirtuales.visitorassistant.core.mvvm.UiEventListener;
import com.mundosvirtuales.visitorassistant.core.mvvm.UiStateListener;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorDtos;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorFlowPresenter;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorFlowState;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorNameNormalizer;

public class VisitorContactViewModel implements VisitorFlowPresenter.View {

    public static class Strings {
        private final String readyMessage;
        private final String cacheHintLoading;
        private final String cacheHintRetry;
        private final String submittingTemplate;

        public Strings(String readyMessage, String cacheHintLoading, String cacheHintRetry, String submittingTemplate) {
            this.readyMessage = readyMessage;
            this.cacheHintLoading = cacheHintLoading;
            this.cacheHintRetry = cacheHintRetry;
            this.submittingTemplate = submittingTemplate;
        }
    }

    private VisitorFlowPresenter presenter;
    private final Strings strings;
    private final SimpleStateStore<VisitorContactUiState> stateStore = new SimpleStateStore<>();
    private final SimpleEventDispatcher<VisitorContactUiEvent> eventDispatcher = new SimpleEventDispatcher<>();
    private VisitorFlowState.Screen lastRenderedScreen;

    public VisitorContactViewModel(VisitorFlowPresenter presenter, Strings strings) {
        this.presenter = presenter;
        this.strings = strings;
    }

    public void attachPresenter(VisitorFlowPresenter presenter) {
        this.presenter = presenter;
    }

    public void observe(UiStateListener<VisitorContactUiState> stateListener,
                        UiEventListener<VisitorContactUiEvent> eventListener) {
        stateStore.observe(stateListener);
        eventDispatcher.observe(eventListener);
    }

    public void start() {
        presenter.start();
    }

    public void onRetryRequested() {
        presenter.onRetry();
    }

    public void onContactTapped(VisitorDtos.ContactSummary contact, String currentVisitorName) {
        String normalizedVisitorName = VisitorNameNormalizer.normalizeOrNull(currentVisitorName);
        if (normalizedVisitorName == null) {
            eventDispatcher.emit(new VisitorContactUiEvent(VisitorContactUiEvent.Type.PROMPT_VISITOR_NAME, contact));
            return;
        }
        presenter.onContactSelected(contact, normalizedVisitorName);
    }

    public boolean onVisitorNameProvided(VisitorDtos.ContactSummary contact, String visitorName) {
        String normalizedVisitorName = VisitorNameNormalizer.normalizeOrNull(visitorName);
        if (normalizedVisitorName == null) {
            eventDispatcher.emit(new VisitorContactUiEvent(VisitorContactUiEvent.Type.INVALID_VISITOR_NAME, contact));
            return false;
        }
        presenter.onContactSelected(contact, normalizedVisitorName);
        return true;
    }

    @Override
    public void render(VisitorFlowState state) {
        boolean maintenance = state.getScreen() == VisitorFlowState.Screen.MAINTENANCE;
        String statusMessage = resolveMessage(state);
        String cacheHint = resolveCacheHint(state);
        boolean success = state.getScreen() == VisitorFlowState.Screen.SUCCESS
                && lastRenderedScreen != VisitorFlowState.Screen.SUCCESS
                && state.getMessage() != null
                && !state.getMessage().trim().isEmpty();

        stateStore.setState(new VisitorContactUiState(
                statusMessage,
                !statusMessage.isEmpty() && !maintenance,
                cacheHint,
                !cacheHint.isEmpty(),
                maintenance,
                state.getMessage(),
                !maintenance,
                isBusy(state),
                state.isRetryEnabled(),
                state.getScreen() == VisitorFlowState.Screen.READY,
                success,
                state.getMessage(),
                state.getContacts(),
                state.getScreen()
        ));
        lastRenderedScreen = state.getScreen();
    }

    private boolean isBusy(VisitorFlowState state) {
        return state.getScreen() == VisitorFlowState.Screen.LOADING
                || state.getScreen() == VisitorFlowState.Screen.SUBMITTING;
    }

    private String resolveMessage(VisitorFlowState state) {
        if (state.getScreen() == VisitorFlowState.Screen.LOADING && !state.isShowingCachedContacts()) {
            return "";
        }
        if (state.getScreen() == VisitorFlowState.Screen.SUCCESS) {
            return "";
        }
        if (state.getScreen() == VisitorFlowState.Screen.SUBMITTING && state.getSelectedContactId() != null) {
            for (VisitorDtos.ContactSummary contact : state.getContacts()) {
                if (state.getSelectedContactId().equals(contact.getId())) {
                    return String.format(strings.submittingTemplate, contact.getDisplayName());
                }
            }
        }
        if (state.getScreen() == VisitorFlowState.Screen.READY && state.isShowingCachedContacts()) {
            return strings.readyMessage;
        }
        if (state.getScreen() == VisitorFlowState.Screen.MAINTENANCE) {
            return "";
        }
        return state.getMessage();
    }

    private String resolveCacheHint(VisitorFlowState state) {
        if (!state.isShowingCachedContacts() || state.getScreen() == VisitorFlowState.Screen.MAINTENANCE) {
            return "";
        }
        if (state.getScreen() == VisitorFlowState.Screen.LOADING) {
            return strings.cacheHintLoading;
        }
        if (state.getScreen() == VisitorFlowState.Screen.READY) {
            return strings.cacheHintRetry;
        }
        return strings.cacheHintLoading;
    }
}
