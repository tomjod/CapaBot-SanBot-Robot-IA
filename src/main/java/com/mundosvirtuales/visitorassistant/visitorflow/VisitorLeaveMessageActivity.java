package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mundosvirtuales.visitorassistant.BuildConfig;
import com.mundosvirtuales.visitorassistant.MySettings;
import com.mundosvirtuales.visitorassistant.R;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.unit.SpeechManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.mundosvirtuales.visitorassistant.MyUtils.concludeSpeak;

public class VisitorLeaveMessageActivity extends TopBaseActivity implements VisitorMessagePresenter.View {

    public static Intent createIntent(Context context) {
        return new Intent(context, VisitorLeaveMessageActivity.class);
    }

    private SpeechManager speechManager;
    private VisitorMessagePresenter presenter;
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
    private EditText messageInput;
    private Button retryButton;
    private Button sendButton;
    private Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(VisitorLeaveMessageActivity.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_message);

        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);

        titleView = findViewById(R.id.visitorMessageTitle);
        statusView = findViewById(R.id.visitorMessageStatus);
        cacheHintView = findViewById(R.id.visitorMessageCacheHint);
        maintenanceOverlay = findViewById(R.id.visitorMessageMaintenanceOverlay);
        maintenanceTitleView = findViewById(R.id.visitorMessageMaintenanceTitle);
        maintenanceMessageView = findViewById(R.id.visitorMessageMaintenanceMessage);
        listHintView = findViewById(R.id.visitorMessageListHint);
        progressBar = findViewById(R.id.visitorMessageProgress);
        contactsList = findViewById(R.id.visitorMessageContacts);
        messageInput = findViewById(R.id.visitorMessageInput);
        retryButton = findViewById(R.id.visitorMessageRetry);
        sendButton = findViewById(R.id.visitorMessageSend);
        finishButton = findViewById(R.id.visitorMessageFinish);

        titleView.setText(R.string.visitor_message_title);
        adapter = new VisitorContactAdapter(this, new ArrayList<ContactListItemViewModel>());
        contactsList.setAdapter(adapter);

        BackendApiClient apiClient = new BackendApiClient(
                BuildConfig.VISITOR_API_BASE_URL,
                buildDeviceId(),
                null,
                buildLocation(),
                getString(R.string.visitor_flow_configuration_error)
        );
        presenter = new VisitorMessagePresenter(
                this,
                apiClient,
                apiClient,
                new SharedPreferencesVisitorContactCacheStore(this),
                message -> {
                    if (speechManager != null) {
                        speechManager.startSpeak(message, MySettings.getSpeakDefaultOption());
                        concludeSpeak(speechManager);
                    }
                },
                getString(R.string.visitor_message_loading),
                getString(R.string.visitor_message_loading_cached),
                getString(R.string.visitor_message_ready),
                getString(R.string.visitor_message_selected),
                getString(R.string.visitor_message_unavailable),
                getString(R.string.visitor_message_failed),
                getString(R.string.visitor_maintenance_message),
                getString(R.string.visitor_message_empty_contacts),
                getString(R.string.visitor_message_validation_contact),
                getString(R.string.visitor_message_validation_text),
                getString(R.string.visitor_message_validation_length),
                getString(R.string.visitor_message_success)
        );

        contactsList.setOnItemClickListener((parent, view, position, id) -> {
            ContactListItemViewModel item = adapter.getItem(position);
            if (item != null) {
                presenter.onContactSelected(item.getContact());
            }
        });
        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                presenter.onMessageChanged(editable == null ? "" : editable.toString());
            }
        });
        retryButton.setOnClickListener(view -> presenter.onRetry());
        sendButton.setOnClickListener(view -> presenter.onSubmit());
        finishButton.setOnClickListener(view -> returnToStart());

        new Handler().post(presenter::start);
    }

    @Override
    public void render(VisitorMessageFlowState state) {
        runOnUiThread(() -> {
            boolean maintenance = state.getScreen() == VisitorMessageFlowState.Screen.MAINTENANCE;
            statusView.setText(resolveMessage(state));
            statusView.setVisibility(maintenance ? View.GONE : View.VISIBLE);
            cacheHintView.setVisibility(state.isShowingCachedContacts() && !maintenance ? View.VISIBLE : View.GONE);
            maintenanceOverlay.setVisibility(maintenance ? View.VISIBLE : View.GONE);
            maintenanceTitleView.setText(R.string.visitor_maintenance_title);
            maintenanceMessageView.setText(state.getMessage());
            listHintView.setVisibility(maintenance ? View.GONE : View.VISIBLE);
            progressBar.setVisibility(isBusy(state) ? View.VISIBLE : View.GONE);
            retryButton.setVisibility(state.isRetryEnabled() ? View.VISIBLE : View.GONE);
            sendButton.setEnabled(!maintenance && state.isSendEnabled());
            messageInput.setEnabled(!maintenance && !isBusy(state) && state.getScreen() != VisitorMessageFlowState.Screen.SUCCESS);
            contactsList.setEnabled(!maintenance && !isBusy(state));
            finishButton.setText(state.getScreen() == VisitorMessageFlowState.Screen.SUCCESS
                    ? R.string.visitor_flow_finish
                    : R.string.visitor_flow_back_to_start);

            if (!messageInput.getText().toString().equals(state.getDraftMessage())) {
                messageInput.setText(state.getDraftMessage());
                messageInput.setSelection(messageInput.getText().length());
            }

            bindContacts(state.getContacts());
        });
    }

    private boolean isBusy(VisitorMessageFlowState state) {
        return state.getScreen() == VisitorMessageFlowState.Screen.LOADING
                || state.getScreen() == VisitorMessageFlowState.Screen.SUBMITTING;
    }

    private String resolveMessage(VisitorMessageFlowState state) {
        if (state.getScreen() == VisitorMessageFlowState.Screen.MAINTENANCE) {
            return "";
        }
        if (state.getScreen() == VisitorMessageFlowState.Screen.SUBMITTING && state.getSelectedContactId() != null) {
            for (VisitorDtos.ContactSummary contact : state.getContacts()) {
                if (state.getSelectedContactId().equals(contact.getId())) {
                    return getString(R.string.visitor_message_submitting, contact.getDisplayName());
                }
            }
        }
        return state.getMessage();
    }

    private void bindContacts(List<VisitorDtos.ContactSummary> contacts) {
        List<VisitorDtos.ContactSummary> sortedContacts = new ArrayList<>(contacts);
        Collections.sort(sortedContacts, new Comparator<VisitorDtos.ContactSummary>() {
            @Override
            public int compare(VisitorDtos.ContactSummary left, VisitorDtos.ContactSummary right) {
                if (left.isAvailable() != right.isAvailable()) {
                    return left.isAvailable() ? -1 : 1;
                }
                String leftName = left.getDisplayName() == null ? "" : left.getDisplayName();
                String rightName = right.getDisplayName() == null ? "" : right.getDisplayName();
                return leftName.compareToIgnoreCase(rightName);
            }
        });

        List<ContactListItemViewModel> items = new ArrayList<>();
        for (VisitorDtos.ContactSummary contact : sortedContacts) {
            items.add(new ContactListItemViewModel(contact, buildChannelsLabel(contact), buildAvailabilityLabel(contact)));
        }
        adapter.clear();
        adapter.addAll(items);
        adapter.notifyDataSetChanged();
    }

    private String buildChannelsLabel(VisitorDtos.ContactSummary contact) {
        return getString(
                R.string.visitor_flow_channels,
                contact.isTelegramAvailable() ? getString(R.string.visitor_flow_available) : getString(R.string.visitor_flow_unavailable_short),
                contact.isEmailAvailable() ? getString(R.string.visitor_flow_available) : getString(R.string.visitor_flow_unavailable_short)
        );
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

    private String buildLocation() {
        if (BuildConfig.VISITOR_LOCATION_LABEL == null) {
            return null;
        }
        String normalized = BuildConfig.VISITOR_LOCATION_LABEL.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void returnToStart() {
        startActivity(new Intent(this, VisitorStartActivity.class));
        finish();
    }

    @Override
    protected void onMainServiceConnected() {
        // No-op.
    }
}
