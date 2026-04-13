package com.mundosvirtuales.visitorassistant.visitorflow;

public final class VisitorNameNormalizer {

    private VisitorNameNormalizer() {
    }

    public static String normalizeOrNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
