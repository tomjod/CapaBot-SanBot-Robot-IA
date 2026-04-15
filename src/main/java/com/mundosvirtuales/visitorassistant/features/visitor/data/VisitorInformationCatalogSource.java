package com.mundosvirtuales.visitorassistant.features.visitor.data;

import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorInformationOption;

import java.util.List;

public interface VisitorInformationCatalogSource {
    List<VisitorInformationOption> getOptions();
}
