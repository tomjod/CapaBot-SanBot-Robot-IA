package com.sanbot.capaBot.Utils;

import android.media.MediaPlayer;
import android.util.Log;

import java.io.IOException;

/**
 * Una clase de utilidad para reproducir archivos de audio locales usando MediaPlayer.
 * Maneja el ciclo de vida completo: preparar, iniciar, detener y liberar recursos.
 */
public class SoundPlayer {
    private static final String TAG = "SoundPlayer";
    private MediaPlayer mediaPlayer;

    /**
     * Interfaz para notificar cuando la reproducción ha terminado.
     */
    public interface OnPlaybackCompleteListener {
        void onCompletion();
    }

    /**
     * Reproduce un archivo de audio desde una ruta de archivo.
     * @param filePath La ruta completa al archivo de audio (ej: /sdcard/speech.mp3).
     * @param listener Un listener opcional para ser notificado cuando la reproducción termine.
     */
    public void play(String filePath, final OnPlaybackCompleteListener listener) {
        stop(); // Detiene y libera cualquier instancia anterior.

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepareAsync(); // Prepara de forma asíncrona para no bloquear.

            mediaPlayer.setOnPreparedListener(mp -> {
                Log.d(TAG, "MediaPlayer preparado. Iniciando reproducción.");
                mp.start();
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Reproducción completada.");
                stop();
                if (listener != null) {
                    listener.onCompletion();
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "Error en MediaPlayer: " + what + ", " + extra);
                stop();
                if (listener != null) {
                    listener.onCompletion(); // Notificamos para que el flujo de la app continúe.
                }
                return false;
            });

        } catch (IOException e) {
            Log.e(TAG, "Error al configurar la fuente de datos de MediaPlayer", e);
            stop();
        }
    }

    /**
     * Detiene la reproducción y libera los recursos del MediaPlayer.
     */
    public void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

