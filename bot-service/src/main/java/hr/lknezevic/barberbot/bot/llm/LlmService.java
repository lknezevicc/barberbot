package hr.lknezevic.barberbot.bot.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.bot.service.ConversationEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private static final int MAX_RETRIES = 2;

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final LlmResponseSanitizer sanitizer;

    public <T> T ask(String systemPrompt, String userMessage,
                     List<ConversationEntry> history, Class<T> type) {
        LlmResponseSanitizer.LlmResponseException lastException = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            String rawResponse = callLlm(systemPrompt, userMessage, history);
            log.debug("LLM raw response (attempt {}): {}", attempt, rawResponse);

            try {
                String validJson = sanitizer.extractAndValidateJson(rawResponse);
                return objectMapper.readValue(validJson, type);
            } catch (LlmResponseSanitizer.LlmResponseException e) {
                lastException = e;
                log.warn("LLM response validation failed (attempt {}/{}): {}",
                        attempt + 1, MAX_RETRIES + 1, e.getMessage());
            } catch (Exception e) {
                log.warn("Failed to deserialize LLM response (attempt {}/{}): {}",
                        attempt + 1, MAX_RETRIES + 1, e.getMessage());
                lastException = new LlmResponseSanitizer.LlmResponseException(
                        "Deserialization failed: " + e.getMessage(), e);
            }
        }

        throw new IllegalStateException(
                "LLM failed after " + (MAX_RETRIES + 1) + " attempts", lastException);
    }

    private String callLlm(String systemPrompt, String userMessage,
                           List<ConversationEntry> history) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemPrompt));

        for (ConversationEntry entry : history) {
            if ("user".equals(entry.role())) {
                messages.add(new UserMessage(entry.content()));
            } else {
                messages.add(new AssistantMessage(entry.content()));
            }
        }

        messages.add(new UserMessage(userMessage));

        return chatClient.prompt(new Prompt(messages)).call().content();
    }
}
