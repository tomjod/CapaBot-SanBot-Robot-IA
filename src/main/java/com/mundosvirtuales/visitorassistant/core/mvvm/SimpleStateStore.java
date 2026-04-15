package com.mundosvirtuales.visitorassistant.core.mvvm;

public class SimpleStateStore<T> {

    private T state;
    private UiStateListener<T> listener;

    public void observe(UiStateListener<T> listener) {
        this.listener = listener;
        if (listener != null && state != null) {
            listener.onStateChanged(state);
        }
    }

    public void setState(T state) {
        this.state = state;
        if (listener != null) {
            listener.onStateChanged(state);
        }
    }

    public T getState() {
        return state;
    }
}
