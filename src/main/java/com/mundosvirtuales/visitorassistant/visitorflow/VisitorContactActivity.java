package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Context;
import android.content.Intent;
import android.app.AlertDialog;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mundosvirtuales.visitorassistant.BuildConfig;
import com.mundosvirtuales.visitorassistant.MySettings;
import com.mundosvirtuales.visitorassistant.R;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.unit.SpeechManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VisitorContactActivity extends TopBaseActivity implements VisitorFlowPresenter.View {

    public static Intent createIntent(Context context, String visitorName) {
        Intent intent = new Intent(context, VisitorContactActivity.class);
        intent.putExtra("name", visitorName);
        return intent;
    }

    private SpeechManager speechManager;
    private VisitorFlowPresenter presenter;
    private VisitorContactAdapter adapter;

    private TextView titleView;
    private TextView statusView;
    private TextView cacheHintView;
    private View maintenanceOverlay;
    private TextView maintenanceTitleView;
    private TextView maintenanceMessageView;
    private TextView listHintView;
    private ProgressBar progressBar;
    private ListView contactsList;
    private Button retryButton;
    private Button finishButton;
    private Handler mainHandler;
    private ExecutorService contactBindingExecutor;
    private int contactBindingGeneration;
    private VisitorFlowState.Screen lastRenderedScreen;
    private String visitorName;
    private VisitorIdleHomeController idleHomeController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(VisitorContactActivity.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_contact);

        mainHandler = new Handler();
        contactBindingExecutor = Executors.newSingleThreadExecutor();
        idleHomeController = new VisitorIdleHomeController(this, 45000L);
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);

        titleView = findViewById(R.id.visitorTitle);
        statusView = findViewById(R.id.visitorStatus);
        cacheHintView = findViewById(R.id.visitorCacheHint);
        maintenanceOverlay = findViewById(R.id.visitorMaintenanceOverlay);
        maintenanceTitleView = findViewById(R.id.visitorMaintenanceTitle);
        maintenanceMessageView = findViewById(R.id.visitorMaintenanceMessage);
        listHintView = findViewById(R.id.visitorListHint);
        progressBar = findViewById(R.id.visitorProgress);
        contactsList = findViewById(R.id.visitorContacts);
        retryButton = findViewById(R.id.visitorRetry);
        finishButton = findViewById(R.id.visitorFinish);

        titleView.setText(R.string.visitor_flow_title);
        visitorName = buildVisitorName();
        adapter = new VisitorContactAdapter(this, new ArrayList<ContactListItemViewModel>());
        contactsList.setAdapter(adapter);

        BackendApiClient apiClient = new BackendApiClient(
                BuildConfig.VISITOR_API_BASE_URL,
                buildDeviceId(),
                visitorName,
                buildLocation(),
                getString(R.string.visitor_flow_configuration_error)
        );
        presenter = new VisitorFlowPresenter(
                this,
                apiClient,
                apiClient,
                new SharedPreferencesVisitorContactCacheStore(this),
                this::speakAsync,
                getString(R.string.visitor_flow_loading),
                getString(R.string.visitor_flow_loading_cached),
                getString(R.string.visitor_flow_ready),
                getString(R.string.visitor_flow_contact_unavailable),
                getString(R.string.visitor_flow_failed),
                getString(R.string.visitor_flow_load_failed),
                getString(R.string.visitor_maintenance_message),
                getString(R.string.visitor_flow_empty_contacts),
                getString(R.string.visitor_flow_success),
                getString(R.string.visitor_name_required)
        );

        contactsList.setOnItemClickListener((parent, view, position, id) -> {
            ContactListItemViewModel item = adapter.getItem(position);
            if (item != null) {
                String normalizedVisitorName = VisitorNameNormalizer.normalizeOrNull(visitorName);
                if (normalizedVisitorName != null) {
                    presenter.onContactSelected(item.getContact(), normalizedVisitorName);
                    return;
                }
                promptVisitorName(item.getContact(), false);
            }
        });

        retryButton.setOnClickListener(view -> presenter.onRetry());
        finishButton.setOnClickListener(view -> returnToBase());

        contactsList.postDelayed(presenter::start, 120);
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
    protected void onDestroy() {
        super.onDestroy();
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        if (contactBindingExecutor != null) {
            contactBindingExecutor.shutdownNow();
        }
    }

    @Override
    public void render(VisitorFlowState state) {
        runOnUiThread(() -> {
            maybeShowSuccessToast(state);
            String statusMessage = resolveMessage(state);
            boolean maintenance = state.getScreen() == VisitorFlowState.Screen.MAINTENANCE;
            statusView.setText(statusMessage);
            statusView.setVisibility(statusMessage.isEmpty() || maintenance ? View.GONE : View.VISIBLE);

            String cacheHint = resolveCacheHint(state);
            cacheHintView.setText(cacheHint);
            cacheHintView.setVisibility(cacheHint.isEmpty() ? View.GONE : View.VISIBLE);
            maintenanceOverlay.setVisibility(maintenance ? View.VISIBLE : View.GONE);
            maintenanceTitleView.setText(R.string.visitor_maintenance_title);
            maintenanceMessageView.setText(state.getMessage());
            listHintView.setVisibility(maintenance ? View.GONE : View.VISIBLE);
            progressBar.setVisibility(isBusy(state) ? View.VISIBLE : View.GONE);
            retryButton.setVisibility(state.isRetryEnabled() ? View.VISIBLE : View.GONE);
            finishButton.setText(state.getScreen() == VisitorFlowState.Screen.SUCCESS
                    ? R.string.visitor_flow_finish
                    : R.string.visitor_flow_back_to_start);

            bindContacts(state.getContacts());
            contactsList.setEnabled(state.getScreen() == VisitorFlowState.Screen.READY);
            lastRenderedScreen = state.getScreen();
        });
    }

    private boolean isBusy(VisitorFlowState state) {
        return state.getScreen() == VisitorFlowState.Screen.LOADING
                || state.getScreen() == VisitorFlowState.Screen.SUBMITTING;
    }

    private String resolveMessage(VisitorFlowState state) {
        if (state.getScreen() == VisitorFlowState.Screen.LOADING && !state.isShowingCachedContacts()) {
            return "";
        }
        if (state.getScreen() == VisitorFlowState.Screen.SUCCESS) {
            return "";
        }
        if (state.getScreen() == VisitorFlowState.Screen.SUBMITTING && state.getSelectedContactId() != null) {
            for (VisitorDtos.ContactSummary contact : state.getContacts()) {
                if (state.getSelectedContactId().equals(contact.getId())) {
                    return getString(R.string.visitor_flow_submitting, contact.getDisplayName());
                }
            }
        }
        if (state.getScreen() == VisitorFlowState.Screen.READY && state.isShowingCachedContacts()) {
            return getString(R.string.visitor_flow_ready);
        }
        if (state.getScreen() == VisitorFlowState.Screen.MAINTENANCE) {
            return "";
        }
        return state.getMessage();
    }

    private void maybeShowSuccessToast(VisitorFlowState state) {
        if (state.getScreen() == VisitorFlowState.Screen.SUCCESS
                && lastRenderedScreen != VisitorFlowState.Screen.SUCCESS
                && state.getMessage() != null
                && !state.getMessage().trim().isEmpty()) {
            Toast.makeText(this, state.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String resolveCacheHint(VisitorFlowState state) {
        if (!state.isShowingCachedContacts() || state.getScreen() == VisitorFlowState.Screen.MAINTENANCE) {
            return "";
        }
        if (state.getScreen() == VisitorFlowState.Screen.LOADING) {
            return getString(R.string.visitor_flow_cache_hint_loading);
        }
        if (state.getScreen() == VisitorFlowState.Screen.READY) {
            return getString(R.string.visitor_flow_cache_hint_retry);
        }
        return getString(R.string.visitor_flow_cache_hint_loading);
    }

    private void bindContacts(List<VisitorDtos.ContactSummary> contacts) {
        final int generation = ++contactBindingGeneration;
        final List<VisitorDtos.ContactSummary> contactsSnapshot = new ArrayList<>(contacts);
        contactBindingExecutor.execute(() -> {
            List<VisitorDtos.ContactSummary> sortedContacts = VisitorContactOrdering.sort(contactsSnapshot);
            List<ContactListItemViewModel> items = new ArrayList<>();
            for (VisitorDtos.ContactSummary contact : sortedContacts) {
                items.add(new ContactListItemViewModel(contact, "", buildAvailabilityLabel(contact)));
            }
            mainHandler.post(() -> {
                if (generation != contactBindingGeneration || isFinishing()) {
                    return;
                }
                adapter.clear();
                adapter.addAll(items);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void speakAsync(String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }
        mainHandler.post(() -> {
            if (speechManager != null && !isFinishing()) {
                speechManager.startSpeak(message, MySettings.getSpeakDefaultOption());
            }
        });
    }

    private String buildAvailabilityLabel(VisitorDtos.ContactSummary contact) {
        return contact.isAvailable()
                ? getString(R.string.visitor_flow_contact_ready)
                : getString(R.string.visitor_flow_contact_unavailable_badge);
    }

    private String buildDeviceId() {
        if (BuildConfig.VISITOR_DEVICE_ID != null && !BuildConfig.VISITOR_DEVICE_ID.trim().isEmpty()) {
            return BuildConfig.VISITOR_DEVICE_ID.trim();
        }
        String model = Build.MODEL == null ? "sanbot" : Build.MODEL;
        return model.trim().replace(' ', '-').toLowerCase();
    }

    private String buildVisitorName() {
        String visitorName = getIntent() != null ? getIntent().getStringExtra("name") : null;
        if (visitorName == null) {
            return null;
        }
        String normalized = visitorName.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String buildLocation() {
        if (BuildConfig.VISITOR_LOCATION_LABEL == null) {
            return null;
        }
        String normalized = BuildConfig.VISITOR_LOCATION_LABEL.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void promptVisitorName(VisitorDtos.ContactSummary contact, boolean retrySubmission) {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint(R.string.visitor_name_input_hint);
        input.setText(visitorName == null ? "" : visitorName);
        input.setSelection(input.getText().length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.visitor_name_dialog_title)
                .setMessage(retrySubmission ? R.string.visitor_name_dialog_message_retry : R.string.visitor_name_dialog_message)
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.visitor_name_dialog_continue, null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String normalizedVisitorName = VisitorNameNormalizer.normalizeOrNull(input.getText().toString());
            if (normalizedVisitorName == null) {
                input.setError(getString(R.string.visitor_name_required));
                return;
            }
            visitorName = normalizedVisitorName;
            dialog.dismiss();
            presenter.onContactSelected(contact, normalizedVisitorName);
        }));
        dialog.show();
    }

    private void returnToBase() {
        Intent intent = new Intent(this, VisitorStartActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onMainServiceConnected() {
        // No-op.
    }
}
