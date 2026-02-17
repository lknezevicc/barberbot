package hr.lknezevic.barberbot.bot.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.bot.service.ConversationEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public <T> T ask(String systemPrompt, String userMessage,
                     List<ConversationEntry> history, Class<T> type) {
        ChatClientRequestSpec spec = chatClient.prompt().system(systemPrompt);

        for (ConversationEntry entry : history) {
            if ("user".equals(entry.role())) {
                spec = spec.user(entry.content());
            } else {
                spec = spec.assistant(entry.content());
            }
        }

        String response = spec.user(userMessage).call().content();
        log.debug("LLM raw response: {}", response);

        try {
            return objectMapper.readValue(response, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse LLM response: " + response, e);
        }
    }
}
