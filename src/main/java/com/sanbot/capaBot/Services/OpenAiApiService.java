package com.sanbot.capaBot.Services;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.http.Streaming;

import com.sanbot.capaBot.Models.IAModels;

public interface OpenAiApiService {
    /**
     * Envía una solicitud de completado de chat al modelo especificado.
     * La cabecera de autorización se añade automáticamente a través del Interceptor en ApiClient.
     * @param request El cuerpo de la solicitud que contiene el modelo y los mensajes.
     * @return Un objeto Call que puede ser ejecutado para obtener la respuesta.
     */
    @POST("/v1/responses ")
    Call<IAModels.ChatResponse> createChatCompletion(@Body IAModels.ChatRequest request);

    // Para pasar de audio a texto
    @Multipart
    @POST("v1/audio/transcriptions")
    Call<IAModels.TranscriptionResponse> createTranscription(
            @Part MultipartBody.Part file,
            @Part("model") RequestBody model,
            @Part("language") RequestBody language,
            @Part("prompt") RequestBody prompt,
            @Part("response_format") RequestBody responseFormat,
            @Part("temperature") RequestBody temperature
    );

    /**
     * Genera audio a partir de un texto de entrada.
     * La respuesta es un flujo de datos binarios (el archivo de audio).
     * @Streaming es CRUCIAL para evitar que Retrofit intente cargar todo el
     * archivo de audio en la memoria a la vez.
     */
    @Streaming
    @POST("v1/audio/speech")
    Call<ResponseBody> createSpeech(@Body IAModels.SpeechRequest request);

}
