package com.mundosvirtuales.visitorassistant.features.visitor.application;

import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorEntryPlan;
import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorEntryRequest;
import com.mundosvirtuales.visitorassistant.features.visitor.domain.VisitorEntryScript;
import com.mundosvirtuales.visitorassistant.visitorflow.VisitorFlowEntryDecider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VisitorEntryCoordinator {

    public VisitorEntryPlan createPlan(VisitorEntryRequest request, VisitorEntryScript script) {
        if (request.isBusy() || !request.isBotReady()) {
            return VisitorEntryPlan.blocked();
        }

        String normalizedName = normalizeName(request.getVisitorName());
        VisitorFlowEntryDecider.EntryTarget entryTarget = VisitorFlowEntryDecider.resolve(
                request.isVisitorFlowEnabled(),
                request.getApiBaseUrl()
        );

        List<String> spokenMessages = new ArrayList<>();
        spokenMessages.add(buildGreeting(normalizedName, script));
        String daypartMessage = buildDaypartGreeting(request.getHourOfDay(), request.getDaypartGreetingChance(), script);
        if (daypartMessage != null) {
            spokenMessages.add(daypartMessage);
        }
        if (entryTarget == VisitorFlowEntryDecider.EntryTarget.GUIDED_VISITOR_FLOW) {
            spokenMessages.add(buildVisitorFlowIntro(normalizedName, script));
        }

        return VisitorEntryPlan.ready(
                normalizedName,
                entryTarget == VisitorFlowEntryDecider.EntryTarget.GUIDED_VISITOR_FLOW
                        ? VisitorEntryPlan.Target.GUIDED_VISITOR_FLOW
                        : VisitorEntryPlan.Target.LEGACY_DIALOG,
                spokenMessages
        );
    }

    private String normalizeName(String visitorName) {
        if (visitorName == null) {
            return null;
        }
        String normalized = visitorName.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String buildGreeting(String normalizedName, VisitorEntryScript script) {
        if (normalizedName == null) {
            return script.getHiMessage();
        }
        return script.getHiMessage() + ", " + normalizedName;
    }

    private String buildVisitorFlowIntro(String normalizedName, VisitorEntryScript script) {
        if (normalizedName == null) {
            return script.getGenericFlowIntro();
        }
        return String.format(Locale.getDefault(), script.getNamedFlowIntroTemplate(), normalizedName);
    }

    private String buildDaypartGreeting(int hourOfDay, double randomValue, VisitorEntryScript script) {
        if (randomValue >= 0.5d) {
            return null;
        }
        if (hourOfDay < 6) {
            return script.getEarlyMorningMessage();
        }
        if (hourOfDay < 12) {
            return script.getMorningMessage();
        }
        if (hourOfDay < 18) {
            return script.getAfternoonMessage();
        }
        return script.getNightMessage();
    }
}
