package com.mundosvirtuales.visitorassistant.features.visitor.data;

public interface VisitorAvailabilityGateway {

    interface Callback {
        void onAvailable();

        void onUnavailable(String message);
    }

    void checkAvailability(Callback callback);
}
