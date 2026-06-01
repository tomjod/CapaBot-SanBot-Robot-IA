package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mundosvirtuales.visitorassistant.BuildConfig;
import com.mundosvirtuales.visitorassistant.MyBaseActivity;
import com.mundosvirtuales.visitorassistant.R;
import com.mundosvirtuales.visitorassistant.app.visitor.VisitorActivityLauncher;
import com.mundosvirtuales.visitorassistant.features.visitor.presentation.start.VisitorStartUiEvent;
import com.mundosvirtuales.visitorassistant.features.visitor.presentation.start.VisitorStartUiState;
import com.mundosvirtuales.visitorassistant.features.visitor.presentation.start.VisitorStartViewModel;
import com.mundosvirtuales.visitorassistant.infra.api.VisitorApiAvailabilityGateway;
import com.sanbot.opensdk.base.TopBaseActivity;

public class VisitorStartActivity extends TopBaseActivity {

    private static final String EXTRA_VISITOR_NAME = "name";

    private Button talkButton;
    private Button leaveMessageButton;
    private Button requestInformationButton;
    private TextView legacyBaseLink;
    private TextView statusView;
    private ProgressBar checkingProgressBar;
    private View maintenanceOverlay;
    private TextView maintenanceTitleView;
    private TextView maintenanceMessageView;
    private Button maintenanceRetryButton;
    private VisitorStartViewModel viewModel;
    private VisitorStartUiState currentState = VisitorStartUiState.checking("");
    private VisitorIdleHomeController idleHomeController;

    public static Intent createIntent(android.content.Context context, String visitorName) {
        Intent intent = new Intent(context, VisitorStartActivity.class);
        intent.putExtra(EXTRA_VISITOR_NAME, visitorName);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(VisitorStartActivity.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_start);

        talkButton = findViewById(R.id.visitorStartTalkToPerson);
        leaveMessageButton = findViewById(R.id.visitorStartLeaveMessage);
        requestInformationButton = findViewById(R.id.visitorStartRequestInformation);
        legacyBaseLink = findViewById(R.id.visitorStartLegacyBaseLink);
        statusView = findViewById(R.id.visitorStartStatus);
        checkingProgressBar = findViewById(R.id.visitorStartCheckingProgress);
        maintenanceOverlay = findViewById(R.id.visitorStartMaintenanceOverlay);
        maintenanceTitleView = findViewById(R.id.visitorStartMaintenanceTitle);
        maintenanceMessageView = findViewById(R.id.visitorStartMaintenanceMessage);
        maintenanceRetryButton = findViewById(R.id.visitorStartMaintenanceRetry);
        idleHomeController = new VisitorIdleHomeController(this, 45000L);

        BackendApiClient apiClient = new BackendApiClient(
                BuildConfig.VISITOR_API_BASE_URL,
                BuildConfig.VISITOR_DEVICE_ID,
                buildVisitorName(),
                BuildConfig.VISITOR_LOCATION_LABEL,
                getString(R.string.visitor_flow_configuration_error)
        );
        viewModel = new VisitorStartViewModel(
                new VisitorApiAvailabilityGateway(apiClient),
                getString(R.string.visitor_start_checking),
                getString(R.string.visitor_start_maintenance_message),
                buildVisitorName()
        );
        viewModel.observe(this::render, this::handleEvent);

        talkButton.setOnClickListener(view -> viewModel.onVisitReasonSelected(VisitorStartNavigation.VisitReason.TALK_TO_PERSON));
        leaveMessageButton.setOnClickListener(view -> viewModel.onVisitReasonSelected(VisitorStartNavigation.VisitReason.LEAVE_MESSAGE));
        requestInformationButton.setOnClickListener(view -> viewModel.onVisitReasonSelected(VisitorStartNavigation.VisitReason.REQUEST_INFORMATION));
        legacyBaseLink.setPaintFlags(legacyBaseLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        legacyBaseLink.setOnClickListener(view -> viewModel.onVisitReasonSelected(VisitorStartNavigation.VisitReason.LEGACY_BASE));
        maintenanceRetryButton.setOnClickListener(view -> viewModel.onRetry());

        VisitorFlowLogger.info(
                "launcher.activityCreated",
                "visitorName=" + buildVisitorName() + ", apiBaseUrl=" + BuildConfig.VISITOR_API_BASE_URL + ", deviceId=" + BuildConfig.VISITOR_DEVICE_ID
        );

        viewModel.start();
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

    public void render(VisitorStartUiState state) {
        runOnUiThread(() -> {
            currentState = state;
            boolean maintenance = state.getScreen() == VisitorStartUiState.Screen.MAINTENANCE;
            boolean checking = state.getScreen() == VisitorStartUiState.Screen.CHECKING;
            boolean receptionEnabled = state.isReceptionAccessEnabled();
            boolean informationEnabled = state.isInformationAccessEnabled();
            boolean legacyEnabled = state.isLegacyAccessEnabled();

            talkButton.setEnabled(receptionEnabled);
            leaveMessageButton.setEnabled(receptionEnabled);
            requestInformationButton.setEnabled(informationEnabled);
            talkButton.setAlpha(receptionEnabled ? 1f : 0.45f);
            leaveMessageButton.setAlpha(receptionEnabled ? 1f : 0.45f);
            requestInformationButton.setAlpha(informationEnabled ? 1f : 0.45f);
            legacyBaseLink.setEnabled(legacyEnabled);
            legacyBaseLink.setAlpha(legacyEnabled ? 1f : 0.5f);

            statusView.setText(checking ? state.getMessage() : "");
            statusView.setVisibility(checking ? View.VISIBLE : View.GONE);
            checkingProgressBar.setVisibility(checking ? View.VISIBLE : View.GONE);

            maintenanceOverlay.setVisibility(maintenance ? View.VISIBLE : View.GONE);
            maintenanceTitleView.setText(R.string.visitor_start_maintenance_title);
            maintenanceMessageView.setText(state.getMessage());
            maintenanceRetryButton.setVisibility(state.isRetryEnabled() ? View.VISIBLE : View.GONE);

            VisitorFlowLogger.info(
                    "launcher.render",
                    "screen=" + state.getScreen()
                            + ", receptionEnabled=" + receptionEnabled
                            + ", informationEnabled=" + informationEnabled
                            + ", legacyEnabled=" + legacyEnabled
                            + ", checking=" + checking
                            + ", maintenance=" + maintenance
            );
        });
    }

    private void handleEvent(VisitorStartUiEvent event) {
        startActivity(VisitorActivityLauncher.createIntent(this, event));
        finish();
    }

    private String buildVisitorName() {
        String visitorName = getIntent() != null ? getIntent().getStringExtra(EXTRA_VISITOR_NAME) : null;
        if (visitorName == null) {
            return null;
        }
        String normalized = visitorName.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    @Override
    protected void onMainServiceConnected() {
        // No-op.
    }
}
