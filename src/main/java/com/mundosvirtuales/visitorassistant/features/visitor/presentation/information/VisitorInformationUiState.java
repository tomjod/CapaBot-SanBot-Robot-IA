package com.mundosvirtuales.visitorassistant.features.visitor.presentation.information;

import com.mundosvirtuales.visitorassistant.features.visitor.ui.VisitorInformationOptionItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisitorInformationUiState {

    private final List<VisitorInformationOptionItem> options;
    private final String selectedOptionId;
    private final int selectedLogoResId;
    private final String selectedTitle;
    private final String selectedSummary;
    private final String selectedDetail;

    public VisitorInformationUiState(List<VisitorInformationOptionItem> options,
                                     String selectedOptionId,
                                     int selectedLogoResId,
                                     String selectedTitle,
                                     String selectedSummary,
                                     String selectedDetail) {
        this.options = Collections.unmodifiableList(new ArrayList<>(options));
        this.selectedOptionId = selectedOptionId;
        this.selectedLogoResId = selectedLogoResId;
        this.selectedTitle = selectedTitle;
        this.selectedSummary = selectedSummary;
        this.selectedDetail = selectedDetail;
    }

    public List<VisitorInformationOptionItem> getOptions() { return options; }
    public String getSelectedOptionId() { return selectedOptionId; }
    public int getSelectedLogoResId() { return selectedLogoResId; }
    public String getSelectedTitle() { return selectedTitle; }
    public String getSelectedSummary() { return selectedSummary; }
    public String getSelectedDetail() { return selectedDetail; }
}
