package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mundosvirtuales.visitorassistant.MySettings;
import com.mundosvirtuales.visitorassistant.R;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.unit.SpeechManager;

import java.util.List;

import static com.mundosvirtuales.visitorassistant.MyUtils.concludeSpeak;

public class VisitorInformationActivity extends TopBaseActivity {

    private final java.util.List<View> optionViews = new java.util.ArrayList<>();

    public static Intent createIntent(Context context) {
        return new Intent(context, VisitorInformationActivity.class);
    }

    private SpeechManager speechManager;
    private ImageView detailLogoView;
    private TextView detailTitleView;
    private TextView detailSummaryView;
    private TextView detailView;
    private VisitorIdleHomeController idleHomeController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(VisitorInformationActivity.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_information);

        idleHomeController = new VisitorIdleHomeController(this, 45000L);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);

        LinearLayout optionsContainer = findViewById(R.id.visitorInformationOptions);
        detailLogoView = findViewById(R.id.visitorInformationDetailLogo);
        detailTitleView = findViewById(R.id.visitorInformationDetailTitle);
        detailSummaryView = findViewById(R.id.visitorInformationDetailSummary);
        detailView = findViewById(R.id.visitorInformationDetail);
        Button talkButton = findViewById(R.id.visitorInformationTalk);
        Button backButton = findViewById(R.id.visitorInformationBack);

        bindOptions(optionsContainer, VisitorInformationCatalog.buildDefault(this));

        talkButton.setOnClickListener(view -> {
            startActivity(VisitorContactActivity.createIntent(this, null));
            finish();
        });
        backButton.setOnClickListener(view -> {
            startActivity(new Intent(this, VisitorStartActivity.class));
            finish();
        });
    }

    private void bindOptions(LinearLayout container, List<VisitorInformationCatalog.Option> options) {
        container.removeAllViews();
        optionViews.clear();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (VisitorInformationCatalog.Option option : options) {
            View optionView = inflater.inflate(R.layout.item_visitor_information_option, container, false);
            ImageView logoView = optionView.findViewById(R.id.visitorInformationOptionLogo);
            TextView titleView = optionView.findViewById(R.id.visitorInformationOptionTitle);
            TextView summaryView = optionView.findViewById(R.id.visitorInformationOptionSummary);

            logoView.setImageResource(option.getLogoResId());
            logoView.setContentDescription(getString(R.string.visitor_information_logo_item_content_description, option.getTitle()));
            titleView.setText(option.getTitle());
            summaryView.setText(option.getSummary());

            optionView.setOnClickListener(view -> showDetail(option, view));
            optionViews.add(optionView);
            container.addView(optionView);
        }

        if (!options.isEmpty() && !optionViews.isEmpty()) {
            showDetail(options.get(0), optionViews.get(0));
        }
    }

    private void showDetail(VisitorInformationCatalog.Option option, View selectedView) {
        for (View optionView : optionViews) {
            optionView.setActivated(optionView == selectedView);
        }

        detailLogoView.setImageResource(option.getLogoResId());
        detailLogoView.setContentDescription(getString(R.string.visitor_information_logo_item_content_description, option.getTitle()));
        detailTitleView.setText(option.getTitle());
        detailSummaryView.setText(option.getSummary());
        detailView.setText(option.getDetail());
        if (speechManager != null) {
            speechManager.startSpeak(option.getDetail(), MySettings.getSpeakDefaultOption());
            concludeSpeak(speechManager);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (idleHomeController != null) {
            idleHomeController.start();
        }
    }

    @Override
    protected void onPause() {
        if (idleHomeController != null) {
            idleHomeController.stop();
        }
        super.onPause();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (idleHomeController != null) {
            idleHomeController.onUserInteraction();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onMainServiceConnected() {
        // No-op.
    }
}
