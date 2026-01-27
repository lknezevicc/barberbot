package hr.lknezevic.barberbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public <T> T ask(String systemPrompt, String userMessage, Class<T> type) {
        String response = chatClient
                .prompt()
                .system(systemPrompt)
                .user(userMessage)
                .call()
                .content();

        try {
            return objectMapper.readValue(response, type);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid LLM response: " + response, e);
        }
    }
}
