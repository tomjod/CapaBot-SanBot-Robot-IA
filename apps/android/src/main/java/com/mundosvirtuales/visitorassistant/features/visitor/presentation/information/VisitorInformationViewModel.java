package com.mundosvirtuales.visitorassistant.features.visitor.presentation.information;

import com.mundosvirtuales.visitorassistant.core.mvvm.SimpleEventDispatcher;
import com.mundosvirtuales.visitorassistant.core.mvvm.SimpleStateStore;
import com.mundosvirtuales.visitorassistant.core.mvvm.UiEventListener;
import com.mundosvirtuales.visitorassistant.core.mvvm.UiStateListener;
import com.mundosvirtuales.visitorassistant.features.visitor.data.VisitorInformationCatalogSource;
import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorInformationOption;
import com.mundosvirtuales.visitorassistant.features.visitor.ui.VisitorInformationOptionItem;

import java.util.ArrayList;
import java.util.List;

public class VisitorInformationViewModel {

    private final VisitorInformationCatalogSource catalogSource;
    private final SimpleStateStore<VisitorInformationUiState> stateStore = new SimpleStateStore<>();
    private final SimpleEventDispatcher<VisitorInformationUiEvent> eventDispatcher = new SimpleEventDispatcher<>();
    private List<VisitorInformationOption> options = new ArrayList<>();

    public VisitorInformationViewModel(VisitorInformationCatalogSource catalogSource) {
        this.catalogSource = catalogSource;
    }

    public void observe(UiStateListener<VisitorInformationUiState> stateListener,
                        UiEventListener<VisitorInformationUiEvent> eventListener) {
        stateStore.observe(stateListener);
        eventDispatcher.observe(eventListener);
    }

    public void start() {
        options = catalogSource.getOptions();
        if (options.isEmpty()) {
            stateStore.setState(new VisitorInformationUiState(new ArrayList<VisitorInformationOptionItem>(), null, 0, "", "", ""));
            return;
        }
        selectOption(options.get(0).getId());
    }

    public void onOptionSelected(String optionId) {
        selectOption(optionId);
    }

    private void selectOption(String optionId) {
        VisitorInformationOption selected = null;
        List<VisitorInformationOptionItem> items = new ArrayList<>();
        for (VisitorInformationOption option : options) {
            items.add(new VisitorInformationOptionItem(option.getId(), option.getTitle(), option.getLogoResId(), option.getSummary()));
            if (option.getId().equals(optionId)) {
                selected = option;
            }
        }
        if (selected == null && !options.isEmpty()) {
            selected = options.get(0);
        }
        if (selected == null) {
            return;
        }
        stateStore.setState(new VisitorInformationUiState(
                items,
                selected.getId(),
                selected.getLogoResId(),
                selected.getTitle(),
                selected.getSummary(),
                selected.getDetail()
        ));
        eventDispatcher.emit(new VisitorInformationUiEvent(selected.getDetail()));
    }
}
