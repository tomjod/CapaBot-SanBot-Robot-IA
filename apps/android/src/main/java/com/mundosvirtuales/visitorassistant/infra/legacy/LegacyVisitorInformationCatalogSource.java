package com.mundosvirtuales.visitorassistant.infra.legacy;

import android.content.Context;

import com.mundosvirtuales.visitorassistant.features.visitor.data.VisitorInformationCatalogSource;
import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorInformationOption;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorInformationCatalog;

import java.util.ArrayList;
import java.util.List;

public class LegacyVisitorInformationCatalogSource implements VisitorInformationCatalogSource {

    private final Context context;

    public LegacyVisitorInformationCatalogSource(Context context) {
        this.context = context;
    }

    @Override
    public List<VisitorInformationOption> getOptions() {
        List<VisitorInformationOption> options = new ArrayList<>();
        for (VisitorInformationCatalog.Option option : VisitorInformationCatalog.buildDefault(context)) {
            options.add(new VisitorInformationOption(
                    option.getId(),
                    option.getTitle(),
                    option.getLogoResId(),
                    option.getSummary(),
                    option.getDetail()
            ));
        }
        return options;
    }
}
