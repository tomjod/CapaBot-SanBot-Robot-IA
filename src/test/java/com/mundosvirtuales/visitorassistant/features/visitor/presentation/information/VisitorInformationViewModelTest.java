package com.mundosvirtuales.visitorassistant.features.visitor.presentation.information;

import com.mundosvirtuales.visitorassistant.features.visitor.data.VisitorInformationCatalogSource;
import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorInformationOption;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VisitorInformationViewModelTest {

    @Test
    public void startSelectsFirstOptionAndEmitsSpeechEvent() {
        VisitorInformationViewModel viewModel = new VisitorInformationViewModel(new FakeCatalogSource(options()));
        List<VisitorInformationUiState> states = new ArrayList<>();
        List<VisitorInformationUiEvent> events = new ArrayList<>();

        viewModel.observe(states::add, events::add);
        viewModel.start();

        assertEquals(1, states.size());
        assertEquals("tour", states.get(0).getSelectedOptionId());
        assertEquals("Tour guiado", states.get(0).getSelectedTitle());
        assertEquals(2, states.get(0).getOptions().size());
        assertEquals(1, events.size());
        assertEquals("Detalle tour", events.get(0).getSpeechText());
    }

    @Test
    public void selectingUnknownOptionFallsBackToFirstAvailableOption() {
        VisitorInformationViewModel viewModel = new VisitorInformationViewModel(new FakeCatalogSource(options()));
        List<VisitorInformationUiState> states = new ArrayList<>();
        List<VisitorInformationUiEvent> events = new ArrayList<>();

        viewModel.observe(states::add, events::add);
        viewModel.start();
        viewModel.onOptionSelected("missing");

        assertEquals(2, states.size());
        assertEquals("tour", states.get(1).getSelectedOptionId());
        assertEquals("Detalle tour", events.get(1).getSpeechText());
    }

    @Test
    public void startPublishesEmptyStateWhenCatalogHasNoOptions() {
        VisitorInformationViewModel viewModel = new VisitorInformationViewModel(new FakeCatalogSource(Collections.<VisitorInformationOption>emptyList()));
        List<VisitorInformationUiState> states = new ArrayList<>();
        List<VisitorInformationUiEvent> events = new ArrayList<>();

        viewModel.observe(states::add, events::add);
        viewModel.start();

        assertEquals(1, states.size());
        assertTrueEmptyState(states.get(0));
        assertEquals(0, events.size());
    }

    private void assertTrueEmptyState(VisitorInformationUiState state) {
        assertEquals(0, state.getOptions().size());
        assertNull(state.getSelectedOptionId());
        assertEquals(0, state.getSelectedLogoResId());
        assertEquals("", state.getSelectedTitle());
        assertEquals("", state.getSelectedSummary());
        assertEquals("", state.getSelectedDetail());
    }

    private List<VisitorInformationOption> options() {
        return Arrays.asList(
                new VisitorInformationOption("tour", "Tour guiado", 1, "Resumen tour", "Detalle tour"),
                new VisitorInformationOption("lab", "Laboratorio", 2, "Resumen lab", "Detalle lab")
        );
    }

    private static class FakeCatalogSource implements VisitorInformationCatalogSource {
        private final List<VisitorInformationOption> options;

        private FakeCatalogSource(List<VisitorInformationOption> options) {
            this.options = options;
        }

        @Override
        public List<VisitorInformationOption> getOptions() {
            return options;
        }
    }
}
