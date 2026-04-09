package com.mundosvirtuales.visitorassistant;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.mundosvirtuales.visitorassistant.Controllers.ChatController;
import com.mundosvirtuales.visitorassistant.Services.OpenAiApiService;
import com.mundosvirtuales.visitorassistant.Services.OpenapiClient;
import com.sanbot.opensdk.base.TopBaseActivity;
import com.sanbot.opensdk.beans.FuncConstant;
import com.sanbot.opensdk.function.beans.LED;
import com.sanbot.opensdk.function.beans.headmotion.RelativeAngleHeadMotion;
import com.sanbot.opensdk.function.beans.speech.Grammar;
import com.sanbot.opensdk.function.beans.speech.RecognizeTextBean;
import com.sanbot.opensdk.function.unit.HardWareManager;
import com.sanbot.opensdk.function.unit.HeadMotionManager;
import com.sanbot.opensdk.function.unit.SpeechManager;
import com.sanbot.opensdk.function.unit.interfaces.speech.RecognizeListener;
import com.sanbot.opensdk.function.unit.interfaces.speech.WakenListener;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.mundosvirtuales.visitorassistant.MyUtils.concludeSpeak;

public class VoiceAgentActivity extends TopBaseActivity {

    private final static String TAG = "VOICE-AGENT";

    // Vistas de la UI
    @BindView(R.id.tvStatus)
    TextView tvStatus;
    @BindView(R.id.tvUserTranscript)
    TextView tvUserTranscript;
    @BindView(R.id.tvAgentResponse)
    TextView tvAgentResponse;
    @BindView(R.id.btnListen)
    Button btnListen;

    // Gestores del SDK de Sanbot
    private SpeechManager speechManager;
    private HeadMotionManager headMotionManager;
    private HardWareManager hardWareManager;
    Handler speechResponseHandler = new Handler();
    String lastRecognizedSentence = "";
    boolean infiniteWakeup = true;


    // Componentes de nuestro Agente de IA
    private ChatController chatController;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    // Estados del Agente para controlar el flujo
    private enum AgentState { IDLE, LISTENING, THINKING, SPEAKING }
    private AgentState currentState = AgentState.IDLE;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        register(VoiceAgentActivity.class);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_agent);
        ButterKnife.bind(this);

        initSdkManagers();
        initOpenAiServices();
        initListeners();
    }

    private void initSdkManagers() {
        speechManager = (SpeechManager) getUnitManager(FuncConstant.SPEECH_MANAGER);
        headMotionManager = (HeadMotionManager) getUnitManager(FuncConstant.HEADMOTION_MANAGER);
        hardWareManager = (HardWareManager) getUnitManager(FuncConstant.HARDWARE_MANAGER);
    }

    private void initOpenAiServices() {
        // Asegúrate de que OpenapiClient esté configurado correctamente
        OpenAiApiService apiService = OpenapiClient.getClient().create(OpenAiApiService.class);
        chatController = new ChatController(apiService);
    }

    void initListeners() {
        //Set wakeup, sleep callback
        speechManager.setOnSpeechListener(new WakenListener() {
            @Override
            public void onWakeUpStatus(boolean b) {

            }

            @Override
            public void onWakeUp() {
                Log.i(TAG, "WAKE UP callback");
            }

            @Override
            public void onSleep() {
                Log.i(TAG, "SLEEP callback");
                if (infiniteWakeup) {
                    //recalling wake up to stay awake (not wake-Up-Listening() that resets the Handler)
                    speechManager.doWakeUp();
                }
            }
        });
        speechManager.setOnSpeechListener(new RecognizeListener() {
            @Override
            public void onRecognizeText(RecognizeTextBean recognizeTextBean) {

            }

            @Override
            public boolean onRecognizeResult(@NonNull Grammar grammar) {
                lastRecognizedSentence = Objects.requireNonNull(grammar.getText()).toLowerCase();

                //IGOR: must not exceed 200ms (or less?) don't trust the documentation(500ms), I had to create an handler
                //separate handler so the function could return quickly true, otherwise the robot answers random things over your answers.
                speechResponseHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Speech recognized: " + lastRecognizedSentence);
                        String response = chatController.sendChatMessageAndGetResponse(lastRecognizedSentence);
                        speechManager.startSpeak(response, MySettings.getSpeakDefaultOption());
                        concludeSpeak(speechManager);
                    }
                });
                return true;
            }

            @Override
            public void onRecognizeVolume(int i) {
            }

            @Override
            public void onStartRecognize() {

            }

            @Override
            public void onStopRecognize() {

            }

            @Override
            public void onError(int i, int i1) {

            }
        });


    }

    private void startListening() {
        updateState(AgentState.LISTENING);
        // El parámetro 'false' indica que es un reconocimiento único.
        speechManager.onRecognizeStart();
    }

    private void processUserText(final String text) {
        updateState(AgentState.THINKING);
        mainThreadHandler.post(() -> tvUserTranscript.setText("Tú dijiste: \"" + text + "\""));

        // Ejecutar la llamada a la API en un hilo secundario para no bloquear la UI
        executor.submit(() -> {
            final String responseText = chatController.sendChatMessageAndGetResponse(text);
            // Devolver el resultado al hilo principal para que el robot hable
            mainThreadHandler.post(() -> speakResponse(responseText));
        });
    }

    private void speakResponse(String text) {
        updateState(AgentState.SPEAKING);
        tvAgentResponse.setText(text);
        // Usamos el startSpeak del SDK, que es más eficiente.
        speechManager.startSpeak(text);
    }

    private void updateState(AgentState newState) {
        currentState = newState;
        mainThreadHandler.post(() -> {
            switch (newState) {
                case IDLE:
                    tvStatus.setText("Presiona el botón para hablar");
                    hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_GREEN));
                    btnListen.setEnabled(true);
                    break;
                case LISTENING:
                    tvStatus.setText("Escuchando...");
                    hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_FLICKER_BLUE));
                    btnListen.setEnabled(false);
                    tvAgentResponse.setText("");
                    tvUserTranscript.setText("");
                    break;
                case THINKING:
                    tvStatus.setText("Pensando...");
                    hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_FLICKER_PURPLE));
                    // Mover la cabeza ligeramente para simular que está "pensando"
                    RelativeAngleHeadMotion thinkingMotion = new RelativeAngleHeadMotion(RelativeAngleHeadMotion.ACTION_DOWN, 5);
                    headMotionManager.doRelativeAngleMotion(thinkingMotion);
                    break;
                case SPEAKING:
                    tvStatus.setText("Hablando...");
                    hardWareManager.setLED(new LED(LED.PART_ALL, LED.MODE_BLUE));
                    break;
            }
        });
    }

    public static boolean isNetworkAvailable(Context context) {
        return ((ConnectivityManager) Objects.requireNonNull(context.getSystemService(Context.CONNECTIVITY_SERVICE))).getActiveNetworkInfo() != null;
    }

    @Override
    protected void onMainServiceConnected() {
        // Este método es requerido por la clase base.
        // Una vez conectado el servicio, actualizamos el estado inicial.
        updateState(AgentState.IDLE);
    }

    @Override
    protected void onDestroy() {
        // Liberar recursos para evitar fugas de memoria y comportamiento inesperado
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        if (speechManager != null) {
            speechManager.onRecognizeStop();
            speechManager.stopSpeak();
        }
        super.onDestroy();
    }
}

