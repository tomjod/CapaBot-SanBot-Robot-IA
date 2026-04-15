package com.mundosvirtuales.visitorassistant.features.visitor.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VisitorEntryPlan {

    public enum Target {
        GUIDED_VISITOR_FLOW,
        LEGACY_DIALOG
    }

    private final boolean shouldStart;
    private final String normalizedVisitorName;
    private final Target target;
    private final List<String> spokenMessages;

    private VisitorEntryPlan(boolean shouldStart,
                             String normalizedVisitorName,
                             Target target,
                             List<String> spokenMessages) {
        this.shouldStart = shouldStart;
        this.normalizedVisitorName = normalizedVisitorName;
        this.target = target;
        this.spokenMessages = Collections.unmodifiableList(new ArrayList<>(spokenMessages));
    }

    public static VisitorEntryPlan blocked() {
        return new VisitorEntryPlan(false, null, Target.LEGACY_DIALOG, Collections.<String>emptyList());
    }

    public static VisitorEntryPlan ready(String normalizedVisitorName,
                                         Target target,
                                         List<String> spokenMessages) {
        return new VisitorEntryPlan(true, normalizedVisitorName, target, spokenMessages);
    }

    public boolean shouldStart() {
        return shouldStart;
    }

    public String getNormalizedVisitorName() {
        return normalizedVisitorName;
    }

    public Target getTarget() {
        return target;
    }

    public List<String> getSpokenMessages() {
        return spokenMessages;
    }
}
