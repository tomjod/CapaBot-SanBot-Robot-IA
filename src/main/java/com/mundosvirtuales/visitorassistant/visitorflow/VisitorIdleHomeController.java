package com.mundosvirtuales.visitorassistant.visitorflow;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

final class VisitorIdleHomeController {

    private final Activity activity;
    private final long timeoutMillis;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = this::returnToTouchHome;

    VisitorIdleHomeController(Activity activity, long timeoutMillis) {
        this.activity = activity;
        this.timeoutMillis = timeoutMillis;
    }

    void start() {
        reset();
    }

    void stop() {
        handler.removeCallbacks(timeoutRunnable);
    }

    void onUserInteraction() {
        reset();
    }

    private void reset() {
        handler.removeCallbacks(timeoutRunnable);
        handler.postDelayed(timeoutRunnable, timeoutMillis);
    }

    private void returnToTouchHome() {
        VisitorFlowLogger.info("idle.returnHome", "activity=" + activity.getClass().getSimpleName() + ", timeoutMs=" + timeoutMillis);
        Intent intent = new Intent(activity, VisitorTouchHomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
        activity.finish();
    }
}
