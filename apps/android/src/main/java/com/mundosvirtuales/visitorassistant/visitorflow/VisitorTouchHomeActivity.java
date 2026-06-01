package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import com.mundosvirtuales.visitorassistant.R;
import com.sanbot.opensdk.base.TopBaseActivity;

public class VisitorTouchHomeActivity extends TopBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(VisitorTouchHomeActivity.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_touch_home);

        View root = findViewById(R.id.visitorTouchHomeRoot);
        root.setOnClickListener(view -> openVisitorStart());
    }

    private void openVisitorStart() {
        startActivity(new Intent(this, VisitorStartActivity.class));
        finish();
    }

    @Override
    protected void onMainServiceConnected() {
        // No-op.
    }
}
