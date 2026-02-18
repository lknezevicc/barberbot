package hr.lknezevic.barberbot.bot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.model.enums.BotCommand;
import hr.lknezevic.barberbot.model.enums.LlmIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class LlmResponseSanitizer {

    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile(
            "```(?:json)?\\s*\\n?(.*?)\\n?\\s*```", Pattern.DOTALL
    );

    private static final Set<String> VALID_INTENTS = Arrays.stream(LlmIntent.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    private static final Set<String> VALID_COMMANDS = Arrays.stream(BotCommand.values())
            .map(BotCommand::getCommand)
            .collect(Collectors.toSet());

    private static final Pattern ISO_DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{2}:\\d{2}$");

    private final ObjectMapper objectMapper;

    public String extractAndValidateJson(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new LlmResponseException("LLM returned empty response");
        }

        if (raw.contains(PromptFactory.CANARY_TOKEN)) {
            throw new LlmResponseException("Canary token leaked in LLM response — possible prompt injection");
        }

        String json = stripCodeFences(raw.trim());

        JsonNode root;
        try {
            root = objectMapper.readTree(json);
        } catch (Exception e) {
            throw new LlmResponseException("LLM response is not valid JSON: " + truncate(json), e);
        }

        validateStructure(root);
        return json;
    }

    private String stripCodeFences(String raw) {
        Matcher matcher = CODE_FENCE_PATTERN.matcher(raw);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return raw;
    }

    private void validateStructure(JsonNode root) {
        if (!root.isObject()) {
            throw new LlmResponseException("LLM response is not a JSON object");
        }

        JsonNode intentNode = root.get("intent");
        if (intentNode == null || !intentNode.isTextual()) {
            throw new LlmResponseException("Missing or non-string 'intent' field");
        }

        String intent = intentNode.asText();
        if (!VALID_INTENTS.contains(intent)) {
            throw new LlmResponseException("Unknown intent: " + intent);
        }

        if ("COMMAND_PROXY".equals(intent)) {
            validateCommandProxy(root);
        }

        if ("BOOKING".equals(intent)) {
            validateBookingDraft(root);
        }
    }

    private void validateCommandProxy(JsonNode root) {
        JsonNode cmdNode = root.get("suggestedCommand");
        if (cmdNode == null || !cmdNode.isTextual() || cmdNode.asText().isBlank()) {
            throw new LlmResponseException("COMMAND_PROXY intent requires a valid suggestedCommand");
        }
        if (!VALID_COMMANDS.contains(cmdNode.asText().trim())) {
            throw new LlmResponseException("Unknown suggestedCommand: " + cmdNode.asText());
        }
    }

    private void validateBookingDraft(JsonNode root) {
        JsonNode draft = root.get("bookingDraft");
        if (draft == null || draft.isNull()) {
            return;
        }
        if (!draft.isObject()) {
            throw new LlmResponseException("bookingDraft must be an object or null");
        }

        validateOptionalFormat(draft, "date", ISO_DATE_PATTERN, "YYYY-MM-DD");
        validateOptionalFormat(draft, "time", TIME_PATTERN, "HH:mm");
    }

    private void validateOptionalFormat(JsonNode parent, String field, Pattern pattern, String expected) {
        JsonNode node = parent.get(field);
        if (node == null || node.isNull()) return;
        if (!node.isTextual()) return;

        String value = node.asText().trim();
        if (!value.isEmpty() && !pattern.matcher(value).matches()) {
            log.warn("bookingDraft.{} has invalid format '{}', expected {}", field, value, expected);
        }
    }

    private String truncate(String s) {
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    public static class LlmResponseException extends RuntimeException {
        public LlmResponseException(String message) {
            super(message);
        }

        public LlmResponseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
