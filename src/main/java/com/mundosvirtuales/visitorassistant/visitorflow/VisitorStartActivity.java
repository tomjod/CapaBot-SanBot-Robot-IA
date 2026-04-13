package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mundosvirtuales.visitorassistant.BuildConfig;
import com.mundosvirtuales.visitorassistant.MyBaseActivity;
import com.mundosvirtuales.visitorassistant.R;
import com.sanbot.opensdk.base.TopBaseActivity;

public class VisitorStartActivity extends TopBaseActivity implements VisitorLauncherPresenter.View {

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
    private VisitorLauncherPresenter presenter;
    private VisitorLauncherState currentState = VisitorLauncherState.checking("");

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

        BackendApiClient apiClient = new BackendApiClient(
                BuildConfig.VISITOR_API_BASE_URL,
                BuildConfig.VISITOR_DEVICE_ID,
                buildVisitorName(),
                BuildConfig.VISITOR_LOCATION_LABEL,
                getString(R.string.visitor_flow_configuration_error)
        );
        presenter = new VisitorLauncherPresenter(
                this,
                apiClient,
                getString(R.string.visitor_start_checking),
                getString(R.string.visitor_start_maintenance_message)
        );

        talkButton.setOnClickListener(view -> route(VisitorStartNavigation.VisitReason.TALK_TO_PERSON));
        leaveMessageButton.setOnClickListener(view -> route(VisitorStartNavigation.VisitReason.LEAVE_MESSAGE));
        requestInformationButton.setOnClickListener(view -> route(VisitorStartNavigation.VisitReason.REQUEST_INFORMATION));
        legacyBaseLink.setPaintFlags(legacyBaseLink.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        legacyBaseLink.setOnClickListener(view -> route(VisitorStartNavigation.VisitReason.LEGACY_BASE));
        maintenanceRetryButton.setOnClickListener(view -> presenter.onRetry());

        VisitorFlowLogger.info(
                "launcher.activityCreated",
                "visitorName=" + buildVisitorName() + ", apiBaseUrl=" + BuildConfig.VISITOR_API_BASE_URL + ", deviceId=" + BuildConfig.VISITOR_DEVICE_ID
        );

        presenter.start();
    }

    @Override
    public void render(VisitorLauncherState state) {
        runOnUiThread(() -> {
            currentState = state;
            boolean maintenance = state.getScreen() == VisitorLauncherState.Screen.MAINTENANCE;
            boolean checking = state.getScreen() == VisitorLauncherState.Screen.CHECKING;
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

    private void route(VisitorStartNavigation.VisitReason visitReason) {
        if (currentState == null) {
            VisitorFlowLogger.warn("launcher.route.blocked", "reason=" + visitReason + ", currentState=null");
            return;
        }

        VisitorStartNavigation.Target target = VisitorStartNavigation.resolveTarget(visitReason);
        VisitorFlowLogger.info("launcher.route.request", "reason=" + visitReason + ", target=" + target + ", screen=" + currentState.getScreen());

        if (target == VisitorStartNavigation.Target.CONTACT_FLOW
                || target == VisitorStartNavigation.Target.MESSAGE_FLOW) {
            if (!currentState.isReceptionAccessEnabled()) {
                VisitorFlowLogger.warn("launcher.route.blocked", "reason=" + visitReason + ", target=" + target + ", receptionAccess=false");
                return;
            }
        }

        if (target == VisitorStartNavigation.Target.INFORMATION_FLOW
                && !currentState.isInformationAccessEnabled()) {
            VisitorFlowLogger.warn("launcher.route.blocked", "reason=" + visitReason + ", target=" + target + ", informationAccess=false");
            return;
        }

        if (target == VisitorStartNavigation.Target.LEGACY_BASE
                && !currentState.isLegacyAccessEnabled()) {
            VisitorFlowLogger.warn("launcher.route.blocked", "reason=" + visitReason + ", target=" + target + ", legacyAccess=false");
            return;
        }

        if (target == VisitorStartNavigation.Target.CONTACT_FLOW) {
            startActivity(VisitorContactActivity.createIntent(this, buildVisitorName()));
            finish();
            return;
        }

        if (target == VisitorStartNavigation.Target.MESSAGE_FLOW) {
            startActivity(VisitorLeaveMessageActivity.createIntent(this, buildVisitorName()));
            finish();
            return;
        }

        if (target == VisitorStartNavigation.Target.INFORMATION_FLOW) {
            startActivity(VisitorInformationActivity.createIntent(this));
            finish();
            return;
        }

        if (target == VisitorStartNavigation.Target.LEGACY_BASE) {
            startActivity(MyBaseActivity.createIntentFromVisitorStart(this));
            finish();
        }
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
