package com.mundosvirtuales.visitorassistant.core.mvvm;

public interface UiStateListener<T> {
    void onStateChanged(T state);
}
