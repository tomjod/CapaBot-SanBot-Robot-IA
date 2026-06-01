package com.mundosvirtuales.visitorassistant.Utils;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Gestiona el motor de reconocimiento de voz nativo de Android.
 * Reemplaza la funcionalidad de reconocimiento en la nube del SDK de Sanbot.
 */
public class VoiceRecognizer {

    private static final String TAG = "VoiceRecognizer";
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private final RecognitionEvents listener;
    private boolean isListening = false;

    public interface RecognitionEvents {
        void onReadyForSpeech();
        void onSpeechResult(String result);
        void onError(String error);
        void onEndOfSpeech();
    }

    public VoiceRecognizer(Context context, RecognitionEvents listener) {
        this.listener = listener;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES"); // Reconocimiento en Español
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Listo para escuchar.");
                isListening = true;
                if (listener != null) listener.onReadyForSpeech();
            }

            @Override
            public void onResults(Bundle results) {
                isListening = false;
                if (listener != null) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        listener.onSpeechResult(matches.get(0));
                    }
                }
            }

            @Override
            public void onEndOfSpeech() {
                isListening = false;
                if(listener != null) listener.onEndOfSpeech();
            }

            @Override
            public void onError(int error) {
                isListening = false;
                if (listener != null) {
                    String errorMessage = getErrorText(error);
                    Log.e(TAG, "Error de reconocimiento: " + errorMessage);
                    listener.onError(errorMessage);
                }
            }

            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onPartialResults(Bundle partialResults) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
    }

    public void startListening() {
        if (!isListening) {
            speechRecognizer.startListening(recognizerIntent);
        }
    }

    public void stopListening() {
        if (isListening) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private String getErrorText(int errorCode) {
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: return "Error de audio";
            case SpeechRecognizer.ERROR_NO_MATCH: return "No te entendí";
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: return "No te escuché hablar";
            default: return "Error de reconocimiento";
        }
    }
}
