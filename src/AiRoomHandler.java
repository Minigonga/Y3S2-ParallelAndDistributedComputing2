import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.locks.*;

/**
 * Handles AI-enhanced room interactions by storing conversation history and
 * communicating with a local language model API.
 */
public class AiRoomHandler {
    private final String prompt;
    private final List<String> conversationHistory;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private final String modelName;
    private final String aiUrl;

    /**
     * Constructs an AI room handler with the given prompt.
     *
     * @param prompt the initial system prompt to guide AI responses
     */
    public AiRoomHandler(String prompt) {
        this.prompt = prompt;
        this.conversationHistory = new ArrayList<>();
        this.modelName = "llama3";
        this.aiUrl = "http://localhost:11434/api/generate"; // URL of the AI, change here if needed
    }

    /**
     * Returns the initial system prompt.
     *
     * @return the AI system prompt
     */
    public String getPrompt() {
        return this.prompt;
    }

    /**
     * Processes a message from a user, updates conversation history,
     * and returns the AI-generated response.
     *
     * @param userMessage the latest message from a user
     * @return the AI's response
     */
    public String processMessage(String userMessage) {
        lock.writeLock().lock();
        try {
            conversationHistory.add("User: " + userMessage);

            StringBuilder fullPrompt = new StringBuilder();
            fullPrompt.append(prompt).append("\n\n");

            for (String msg : conversationHistory) {
                fullPrompt.append(msg).append("\n");
            }

            String botResponse = getLlmResponseFromOllama(fullPrompt.toString());

            conversationHistory.add("Bot: " + botResponse);

            return botResponse;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Sends the full prompt and history to the local LLM API and extracts the response.
     *
     * @param fullPrompt the complete prompt to send to the AI
     * @return the parsed AI response or error message
     */
    private String getLlmResponseFromOllama(String fullPrompt) {
        try {
            String jsonPayload = String.format(
                "{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}",
                modelName,
                escapeJson(fullPrompt)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(aiUrl))
                    .header("Content-Type", "application/json")
                    .POST(BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = httpClient.send(
                request,
                BodyHandlers.ofString(StandardCharsets.UTF_8)
            );

            String body = response.body();
            int start = body.indexOf("\"response\":\"") + 12;
            int end = body.indexOf("\"", start);
            if (start > 11 && end > start) {
                String raw = body.substring(start, end);
                return raw.replace("\\n", "\n").replace("\\\"", "\"");
            } else {
                return "[Error: Unexpected response format from LLM]";
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "[Error: Failed to get AI response. " + e.getMessage() + "]";
        }
    }

    /**
     * Escapes special characters in a string for safe JSON formatting.
     *
     * @param input the string to escape
     * @return the escaped string
     */
    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
    }
}
