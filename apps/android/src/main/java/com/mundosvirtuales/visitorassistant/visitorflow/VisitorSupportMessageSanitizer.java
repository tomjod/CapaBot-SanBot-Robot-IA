package com.mundosvirtuales.visitorassistant.visitorflow;

import java.util.Locale;

final class VisitorSupportMessageSanitizer {

    private VisitorSupportMessageSanitizer() {
    }

    static String normalize(String message) {
        if (message == null) {
            return null;
        }
        String normalized = message.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    static boolean looksTechnicalFailure(String message) {
        String normalized = normalize(message);
        if (normalized == null) {
            return false;
        }
        String lowerCase = normalized.toLowerCase(Locale.ROOT);
        return lowerCase.contains("backend")
                || lowerCase.contains("configur")
                || lowerCase.contains("servidor")
                || lowerCase.contains("api")
                || lowerCase.contains("http")
                || lowerCase.contains("conex")
                || lowerCase.contains("timeout")
                || lowerCase.contains("network")
                || lowerCase.contains("socket");
    }
}
