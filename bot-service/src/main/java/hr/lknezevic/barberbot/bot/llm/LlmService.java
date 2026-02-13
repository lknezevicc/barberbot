package hr.lknezevic.barberbot.bot.llm;

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

        log.debug("LLM raw response: {}", response);

        try {
            return objectMapper.readValue(response, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse LLM response: " + response, e);
        }
    }
}
