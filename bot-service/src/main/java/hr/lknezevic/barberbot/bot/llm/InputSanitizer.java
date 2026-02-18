package hr.lknezevic.barberbot.bot.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
public class InputSanitizer {

    private static final int MAX_LENGTH = 500;

    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+instructions?"),
            Pattern.compile("(?i)forget\\s+(all\\s+)?previous"),
            Pattern.compile("(?i)you\\s+are\\s+now"),
            Pattern.compile("(?i)new\\s+instructions?:"),
            Pattern.compile("(?i)system\\s*:\\s*"),
            Pattern.compile("(?i)\\bpretend\\b.*\\byou\\s+are\\b"),
            Pattern.compile("(?i)act\\s+as\\s+if"),
            Pattern.compile("(?i)override\\s+(system|rules|instructions)"),
            Pattern.compile("(?i)zanemari\\s+(prethodne|sve)\\s+(upute|instrukcije|pravila)"),
            Pattern.compile("(?i)nove\\s+instrukcije"),
            Pattern.compile("(?i)ti\\s+si\\s+sada"),
            Pattern.compile("(?i)ponašaj\\s+se\\s+kao")
    );

    public SanitizationResult sanitize(String input) {
        if (input == null || input.isBlank()) {
            return new SanitizationResult("", false);
        }

        String trimmed = input.strip();
        boolean injectionSuspected = false;

        if (trimmed.length() > MAX_LENGTH) {
            log.warn("User input truncated from {} to {} chars", trimmed.length(), MAX_LENGTH);
            trimmed = trimmed.substring(0, MAX_LENGTH);
        }

        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(trimmed).find()) {
                injectionSuspected = true;
                log.warn("Potential prompt injection detected: pattern={}", pattern.pattern());
                break;
            }
        }

        return new SanitizationResult(trimmed, injectionSuspected);
    }

    public record SanitizationResult(String text, boolean injectionSuspected) {}
}
