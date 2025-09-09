package com.sanbot.capaBot.Controllers;

import com.sanbot.capaBot.Models.IAModels;
import com.sanbot.capaBot.Services.OpenAiApiService;

import okhttp3.ResponseBody;
import retrofit2.Call;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Encapsula la lógica para generar audio desde texto y guardarlo en un archivo.
 */
public class SpeechGenerator {

    private final OpenAiApiService apiService;

    public SpeechGenerator(OpenAiApiService apiService) {
        this.apiService = apiService;
    }

    /**
     * Llama a la API para generar el audio.
     * @param request El objeto con el texto, modelo y voz.
     * @return Un objeto Call de Retrofit que puede ser ejecutado.
     */
    public Call<ResponseBody> generateSpeech(IAModels.SpeechRequest request) {
        return apiService.createSpeech(request);
    }

    /**
     * Método de ayuda para guardar el cuerpo de la respuesta (el audio) en un archivo.
     *
     * @param body El ResponseBody recibido de una llamada exitosa de Retrofit.
     * @param outputFile El archivo donde se guardará el audio (ej. new File("speech.mp3")).
     * @return true si se guardó correctamente, false en caso contrario.
     */
    public boolean saveToFile(ResponseBody body, File outputFile) {
        try (InputStream inputStream = body.byteStream();
             OutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] fileReader = new byte[4096];
            long fileSizeDownloaded = 0;

            while (true) {
                int read = inputStream.read(fileReader);
                if (read == -1) {
                    break;
                }
                outputStream.write(fileReader, 0, read);
                fileSizeDownloaded += read;
            }

            outputStream.flush();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
