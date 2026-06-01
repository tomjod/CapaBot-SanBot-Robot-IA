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
import com.mundosvirtuales.visitorassistant.features.visitor.presentation.information.VisitorInformationUiEvent;
import com.mundosvirtuales.visitorassistant.features.visitor.presentation.information.VisitorInformationUiState;
import com.mundosvirtuales.visitorassistant.features.visitor.presentation.information.VisitorInformationViewModel;
import com.mundosvirtuales.visitorassistant.features.visitor.ui.VisitorInformationOptionItem;
import com.mundosvirtuales.visitorassistant.infra.legacy.LegacyVisitorInformationCatalogSource;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.unit.SpeechManager;

import java.util.List;

import static com.mundosvirtuales.visitorassistant.MyUtils.concludeSpeak;

public class VisitorInformationActivity extends TopBaseActivity {

    public static Intent createIntent(Context context) {
        return new Intent(context, VisitorInformationActivity.class);
    }

    private SpeechManager speechManager;
    private LinearLayout optionsContainer;
    private ImageView detailLogoView;
    private TextView detailTitleView;
    private TextView detailSummaryView;
    private TextView detailView;
    private VisitorIdleHomeController idleHomeController;
    private VisitorInformationViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(VisitorInformationActivity.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_information);

        idleHomeController = new VisitorIdleHomeController(this, 45000L);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);

        optionsContainer = findViewById(R.id.visitorInformationOptions);
        detailLogoView = findViewById(R.id.visitorInformationDetailLogo);
        detailTitleView = findViewById(R.id.visitorInformationDetailTitle);
        detailSummaryView = findViewById(R.id.visitorInformationDetailSummary);
        detailView = findViewById(R.id.visitorInformationDetail);
        Button talkButton = findViewById(R.id.visitorInformationTalk);
        Button backButton = findViewById(R.id.visitorInformationBack);

        viewModel = new VisitorInformationViewModel(new LegacyVisitorInformationCatalogSource(this));
        viewModel.observe(this::render, this::handleEvent);

        talkButton.setOnClickListener(view -> {
            startActivity(VisitorContactActivity.createIntent(this, null));
            finish();
        });
        backButton.setOnClickListener(view -> {
            startActivity(new Intent(this, VisitorStartActivity.class));
            finish();
        });

        viewModel.start();
    }

    public void render(VisitorInformationUiState state) {
        runOnUiThread(() -> {
            bindOptions(optionsContainer, state.getOptions(), state.getSelectedOptionId());
            detailLogoView.setImageResource(state.getSelectedLogoResId());
            if (state.getSelectedTitle() == null || state.getSelectedTitle().trim().isEmpty()) {
                detailLogoView.setContentDescription(null);
            } else {
                detailLogoView.setContentDescription(getString(R.string.visitor_information_logo_item_content_description, state.getSelectedTitle()));
            }
            detailTitleView.setText(state.getSelectedTitle());
            detailSummaryView.setText(state.getSelectedSummary());
            detailView.setText(state.getSelectedDetail());
        });
    }

    private void bindOptions(LinearLayout container, List<VisitorInformationOptionItem> options, String selectedOptionId) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (VisitorInformationOptionItem option : options) {
            View optionView = inflater.inflate(R.layout.item_visitor_information_option, container, false);
            ImageView logoView = optionView.findViewById(R.id.visitorInformationOptionLogo);
            TextView titleView = optionView.findViewById(R.id.visitorInformationOptionTitle);
            TextView summaryView = optionView.findViewById(R.id.visitorInformationOptionSummary);

            logoView.setImageResource(option.getLogoResId());
            logoView.setContentDescription(getString(R.string.visitor_information_logo_item_content_description, option.getTitle()));
            titleView.setText(option.getTitle());
            summaryView.setText(option.getSummary());

            optionView.setActivated(option.getId().equals(selectedOptionId));
            optionView.setOnClickListener(view -> viewModel.onOptionSelected(option.getId()));
            container.addView(optionView);
        }
    }

    private void handleEvent(VisitorInformationUiEvent event) {
        if (speechManager != null) {
            speechManager.startSpeak(event.getSpeechText(), MySettings.getSpeakDefaultOption());
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
