package com.sanbot.capaBot.Models;
import com.google.gson.annotations.SerializedName;

import java.util.Dictionary;
import java.util.List;


public class IAModels {
    // --- Clases para el Request (Lo que enviamos a OpenAI) ---

    public static class ChatRequest {
        @SerializedName("model")
        private String model;

        @SerializedName("input")
        private List<ChatMessage> chatMessages;

        @SerializedName("temperature")
        private double temperature;


        public ChatRequest(String model, List<ChatMessage> chatMessages, double temperature) {
            this.model = model;
            this.chatMessages = chatMessages;
            this.temperature = temperature;
        }
    }

    public static class SpeechRequest {

        @SerializedName("model")
        private String model;

        @SerializedName("input")
        private String input;

        @SerializedName("voice")
        private String voice;

        // Campos opcionales
        @SerializedName("response_format")
        private String responseFormat; // ej. "mp3", "opus"

        @SerializedName("speed")
        private Float speed; // ej. 1.0f

        /**
         * Constructor para los campos requeridos.
         * @param model "tts-1", "tts-1-hd", etc.
         * @param input El texto a convertir.
         * @param voice "alloy", "echo", "fable", etc.
         */
        public SpeechRequest(String model, String input, String voice) {
            this.model = model;
            this.input = input;
            this.voice = voice;
        }

        // --- Setters para los campos opcionales ---

        public void setResponseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
        }

        public void setSpeed(Float speed) {
            this.speed = speed;
        }
    }


    public static class Tool {
        @SerializedName("type")
        private String type;

        @SerializedName("name")
        private String name;

        @SerializedName("description")
        private String description;

        @SerializedName("strict")
        private boolean strict;

        @SerializedName("parameters")
        private Dictionary parameters;
    }


    public static class ChatMessage {
        @SerializedName("role")
        private String role;

        @SerializedName("content")
        private String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }


    // --- Clases para el Response (Lo que recibimos de OpenAI) ---


    public static class TranscriptionResponse {
        @SerializedName("text")
        private String text;

        public String getText() {
            return text;
        }
    }

    public static class ChatResponse {

        @SerializedName("output")
        private List<OutputMessage> output;


        public static class OutputMessage {
            @SerializedName("content")
            private List<ContentBlock> content;
        }


        public static class ContentBlock {
            @SerializedName("text")
            private String text;
        }

        /**
         * Navega de forma segura a través de las listas anidadas para obtener
         * el texto del primer mensaje de la respuesta.
         *
         * @return El texto del mensaje como un String, o null si no se encuentra.
         */
        public String getFirstMessageText() {
            try {
                // Comprueba si las listas no son nulas y no están vacías antes de acceder a ellas
                if (output != null && !output.isEmpty()) {
                    OutputMessage firstMessage = output.get(0);
                    if (firstMessage != null && firstMessage.content != null && !firstMessage.content.isEmpty()) {
                        ContentBlock firstContent = firstMessage.content.get(0);
                        if (firstContent != null) {
                            return firstContent.text;
                        }
                    }
                }
            } catch (Exception e) {
                // Captura cualquier excepción inesperada (como IndexOutOfBounds) y la ignora
                e.printStackTrace();
            }
            // Si no se encuentra el texto por cualquier razón, devuelve null
            return null;
        }
    }
}