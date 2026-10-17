package hr.lknezevic.barberbot.bot.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LlmResponseSanitizerTest {

    private final LlmResponseSanitizer sanitizer = new LlmResponseSanitizer(new ObjectMapper());

    @Test
    void validBookingJson_passes() {
        String json = """
                {"intent":"BOOKING","bookingDraft":{"service":"sisanje","date":"2026-06-01","time":"15:00","barber":"Marko"},"suggestedCommand":null,"reply":null}""";
        String result = sanitizer.extractAndValidateJson(json);
        assertThat(result).contains("BOOKING");
    }

    @Test
    void validSmallTalkJson_passes() {
        String json = """
                {"intent":"SMALLTALK","bookingDraft":null,"suggestedCommand":null,"reply":"Bok! Kako si?"}""";
        String result = sanitizer.extractAndValidateJson(json);
        assertThat(result).contains("SMALLTALK");
    }

    @Test
    void validCommandProxyJson_passes() {
        String json = """
                {"intent":"COMMAND_PROXY","bookingDraft":null,"suggestedCommand":"/my_bookings","reply":null}""";
        String result = sanitizer.extractAndValidateJson(json);
        assertThat(result).contains("COMMAND_PROXY");
    }

    @Test
    void codeFences_stripped() {
        String raw = """
                ```json
                {"intent":"UNKNOWN","bookingDraft":null,"suggestedCommand":null,"reply":"Nisam siguran."}
                ```""";
        String result = sanitizer.extractAndValidateJson(raw);
        assertThat(result).contains("UNKNOWN");
    }

    @Test
    void canaryToken_throwsException() {
        String json = """
                {"intent":"UNKNOWN","bookingDraft":null,"suggestedCommand":null,"reply":"Token: XBARBER-9f2a4e"}""";
        assertThatThrownBy(() -> sanitizer.extractAndValidateJson(json))
                .isInstanceOf(LlmResponseSanitizer.LlmResponseException.class)
                .hasMessageContaining("Canary token leaked");
    }

    @Test
    void nullInput_throwsException() {
        assertThatThrownBy(() -> sanitizer.extractAndValidateJson(null))
                .isInstanceOf(LlmResponseSanitizer.LlmResponseException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void blankInput_throwsException() {
        assertThatThrownBy(() -> sanitizer.extractAndValidateJson("   "))
                .isInstanceOf(LlmResponseSanitizer.LlmResponseException.class)
                .hasMessageContaining("empty response");
    }

    @Test
    void invalidJson_throwsException() {
        assertThatThrownBy(() -> sanitizer.extractAndValidateJson("not json at all"))
                .isInstanceOf(LlmResponseSanitizer.LlmResponseException.class)
                .hasMessageContaining("not valid JSON");
    }

    @Test
    void unknownIntent_throwsException() {
        String json = """
                {"intent":"HACKING","bookingDraft":null,"suggestedCommand":null,"reply":null}""";
        assertThatThrownBy(() -> sanitizer.extractAndValidateJson(json))
                .isInstanceOf(LlmResponseSanitizer.LlmResponseException.class)
                .hasMessageContaining("Unknown intent");
    }

    @Test
    void missingIntent_throwsException() {
        String json = """
                {"bookingDraft":null,"suggestedCommand":null,"reply":null}""";
        assertThatThrownBy(() -> sanitizer.extractAndValidateJson(json))
                .isInstanceOf(LlmResponseSanitizer.LlmResponseException.class)
                .hasMessageContaining("Missing or non-string 'intent'");
    }

    @Test
    void commandProxy_withoutSuggestedCommand_throwsException() {
        String json = """
                {"intent":"COMMAND_PROXY","bookingDraft":null,"suggestedCommand":null,"reply":null}""";
        assertThatThrownBy(() -> sanitizer.extractAndValidateJson(json))
                .isInstanceOf(LlmResponseSanitizer.LlmResponseException.class)
                .hasMessageContaining("suggestedCommand");
    }

    @Test
    void commandProxy_withInvalidCommand_throwsException() {
        String json = """
                {"intent":"COMMAND_PROXY","bookingDraft":null,"suggestedCommand":"/hack","reply":null}""";
        assertThatThrownBy(() -> sanitizer.extractAndValidateJson(json))
                .isInstanceOf(LlmResponseSanitizer.LlmResponseException.class)
                .hasMessageContaining("Unknown suggestedCommand");
    }

    @Test
    void bookingDraft_invalidDateFormat_stillPasses_butLogsWarning() {
        String json = """
                {"intent":"BOOKING","bookingDraft":{"service":"sisanje","date":"01-06-2026","time":"15:00","barber":"M"},"suggestedCommand":null,"reply":null}""";
        String result = sanitizer.extractAndValidateJson(json);
        assertThat(result).contains("BOOKING");
    }

    @Test
    void nonObjectResponse_throwsException() {
        assertThatThrownBy(() -> sanitizer.extractAndValidateJson("[1,2,3]"))
                .isInstanceOf(LlmResponseSanitizer.LlmResponseException.class)
                .hasMessageContaining("not a JSON object");
    }
}
