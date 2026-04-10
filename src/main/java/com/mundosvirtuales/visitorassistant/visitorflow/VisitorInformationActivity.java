package com.mundosvirtuales.visitorassistant.visitorflow;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
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

    public static Intent createIntent(Context context) {
        return new Intent(context, VisitorInformationActivity.class);
    }

    private SpeechManager speechManager;
    private TextView detailView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        register(VisitorInformationActivity.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_information);

        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);

        LinearLayout optionsContainer = findViewById(R.id.visitorInformationOptions);
        detailView = findViewById(R.id.visitorInformationDetail);
        Button talkButton = findViewById(R.id.visitorInformationTalk);
        Button backButton = findViewById(R.id.visitorInformationBack);

        detailView.setText(R.string.visitor_information_initial_detail);
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
        for (VisitorInformationCatalog.Option option : options) {
            Button button = new Button(this);
            button.setAllCaps(false);
            button.setBackgroundResource(R.drawable.visitor_secondary_button);
            button.setTextColor(getResources().getColor(R.color.colorPrimaryDark));
            button.setText(option.getTitle() + "\n" + option.getSummary());
            button.setTextSize(18f);
            button.setPadding(24, 20, 24, 20);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.topMargin = getResources().getDimensionPixelSize(R.dimen.visitor_spacing_md);
            button.setLayoutParams(params);
            button.setOnClickListener(view -> showDetail(option));
            container.addView(button);
        }
    }

    private void showDetail(VisitorInformationCatalog.Option option) {
        detailView.setText(option.getDetail());
        if (speechManager != null) {
            speechManager.startSpeak(option.getDetail(), MySettings.getSpeakDefaultOption());
            concludeSpeak(speechManager);
        }
    }

    @Override
    protected void onMainServiceConnected() {
        // No-op.
    }
}
