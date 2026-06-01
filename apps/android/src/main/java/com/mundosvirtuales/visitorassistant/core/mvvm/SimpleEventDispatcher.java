package com.mundosvirtuales.visitorassistant.core.mvvm;

public class SimpleEventDispatcher<T> {

    private UiEventListener<T> listener;

    public void observe(UiEventListener<T> listener) {
        this.listener = listener;
    }

    public void emit(T event) {
        if (listener != null) {
            listener.onEvent(event);
        }
    }
}
