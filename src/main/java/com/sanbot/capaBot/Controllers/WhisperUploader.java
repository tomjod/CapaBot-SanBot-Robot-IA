package com.sanbot.capaBot.Controllers;

import com.sanbot.capaBot.Models.IAModels;
import com.sanbot.capaBot.Services.OpenAiApiService;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;

import java.io.File;

/**
 * Encapsula la lógica para crear y ejecutar una petición de transcripción a la API de Whisper
 * usando una sintaxis compatible con OkHttp 3.12.x.
 */
public class WhisperUploader {

    private final OpenAiApiService apiService;
    private static final MediaType MEDIA_TYPE_AUDIO = MediaType.parse("audio/*");
    private static final MediaType MEDIA_TYPE_TEXT = MediaType.parse("text/plain");

    public WhisperUploader(OpenAiApiService apiService) {
        this.apiService = apiService;
    }

    public Call<IAModels.TranscriptionResponse> transcribe(
            File audioFile,
            String model,
            String language,
            String prompt,
            String responseFormat,
            Float temperature) throws Exception {

        if (audioFile == null || !audioFile.exists()) {
            throw new Exception("El archivo de audio no existe o no se puede leer.");
        }

        // 1. Crear el RequestBody para el archivo de audio
        RequestBody requestFile = RequestBody.create(MEDIA_TYPE_AUDIO, audioFile);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", audioFile.getName(), requestFile);

        // 2. Crear los RequestBody para los otros parámetros de texto
        RequestBody modelPart = RequestBody.create(MEDIA_TYPE_TEXT, model);
        RequestBody languagePart = (language != null) ? RequestBody.create(MEDIA_TYPE_TEXT, language) : null;
        RequestBody promptPart = (prompt != null) ? RequestBody.create(MEDIA_TYPE_TEXT, prompt) : null;
        RequestBody responseFormatPart = (responseFormat != null) ? RequestBody.create(MEDIA_TYPE_TEXT, responseFormat) : null;
        RequestBody temperaturePart = (temperature != null) ? RequestBody.create(MEDIA_TYPE_TEXT, String.valueOf(temperature)) : null;

        // 3. Llamar al método de la interfaz de Retrofit
        return apiService.createTranscription(filePart, modelPart, languagePart, promptPart, responseFormatPart, temperaturePart);
    }
}
