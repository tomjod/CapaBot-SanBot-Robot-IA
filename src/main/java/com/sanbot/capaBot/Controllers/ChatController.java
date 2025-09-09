package com.sanbot.capaBot.Controllers;

import com.sanbot.capaBot.Models.IAModels;
import com.sanbot.capaBot.Services.OpenAiApiService;

import retrofit2.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestiona la lógica de la conversación con la API de Chat de OpenAI.
 * Mantiene el historial de mensajes para dar contexto a la conversación.
 */
public class ChatController {
    final String systemPrompt = "" +
            "Act as a helpful assistant. Provide concise answers that are direct and to the point. " +
            "Avoid unnecessary details, explanations, or examples unless specifically requested. " +
            "Keep responses focused on the core information needed to address the query effectively." +
            "Response always in chilean spanish";

    private final OpenAiApiService apiService;
    private final List<IAModels.ChatMessage> messageHistory;

    public ChatController(OpenAiApiService apiService) {
        this.apiService = apiService;
        this.messageHistory = new ArrayList<>();
        // Inicializa la conversación con un mensaje de sistema para definir el comportamiento del asistente
        this.messageHistory.add(new IAModels.ChatMessage("system", systemPrompt));
    }

    /**
     * Envía un mensaje del usuario a la API y devuelve la respuesta del asistente.
     *
     * @param userText El texto del mensaje del usuario.
     * @return El texto de la respuesta del asistente, o un mensaje de error si falla.
     */
    public String sendChatMessageAndGetResponse(String userText) {
        // 1. Añade el nuevo mensaje del usuario al historial
        messageHistory.add(new IAModels.ChatMessage("user", userText));

        // 2. Prepara la petición con el modelo y el historial completo de mensajes
        IAModels.ChatRequest request = new IAModels.ChatRequest("gpt-4o-mini", messageHistory, 0.2);

        try {
            // 3. Realiza la llamada a la API (esta llamada es síncrona/bloqueante)
            Response<IAModels.ChatResponse> response = apiService.createChatCompletion(request).execute();

            if (response.isSuccessful() && response.body() != null) {
                String assistantText = response.body().getFirstMessageText();

                if (assistantText != null && !assistantText.isEmpty()) {
                    // 4. Añade la respuesta del asistente al historial para la próxima vez
                    messageHistory.add(new IAModels.ChatMessage("assistant", assistantText));
                    return assistantText;
                } else {
                    return "No he recibido una respuesta válida.";
                }
            } else {
                // Maneja el caso de una respuesta no exitosa de la API
                String errorBody = response.errorBody() != null ? response.errorBody().string() : "Cuerpo de error vacío";
                System.err.println("Error en la API: " + response.code() + " - " + errorBody);
                return "Lo siento, ha ocurrido un error al contactar con la IA.";
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "Lo siento, ha ocurrido un error de red.";
        }
    }

    /**
     * Limpia el historial de la conversación para empezar de nuevo.
     */
    public void resetConversation() {
        messageHistory.clear();
        messageHistory.add(new IAModels.ChatMessage("system", systemPrompt));
    }
}
