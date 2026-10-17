package hr.lknezevic.barberbot.model.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class BotCommandTest {

    @Test
    void fromText_start() {
        Optional<BotCommand> result = BotCommand.fromText("/start");
        assertThat(result).contains(BotCommand.START);
    }

    @Test
    void fromText_myBookings() {
        Optional<BotCommand> result = BotCommand.fromText("/my_bookings");
        assertThat(result).contains(BotCommand.MY_BOOKINGS);
    }

    @Test
    void fromText_cancel() {
        Optional<BotCommand> result = BotCommand.fromText("/cancel");
        assertThat(result).contains(BotCommand.CANCEL);
    }

    @Test
    void fromText_help() {
        Optional<BotCommand> result = BotCommand.fromText("/help");
        assertThat(result).contains(BotCommand.HELP);
    }

    @Test
    void fromText_withTrailingText() {
        Optional<BotCommand> result = BotCommand.fromText("/start some extra text");
        assertThat(result).contains(BotCommand.START);
    }

    @Test
    void fromText_caseInsensitive() {
        Optional<BotCommand> result = BotCommand.fromText("/HELP");
        assertThat(result).contains(BotCommand.HELP);
    }

    @Test
    void fromText_null_returnsEmpty() {
        assertThat(BotCommand.fromText(null)).isEmpty();
    }

    @Test
    void fromText_unknownCommand_returnsEmpty() {
        assertThat(BotCommand.fromText("/unknown")).isEmpty();
    }

    @Test
    void fromText_plainText_returnsEmpty() {
        assertThat(BotCommand.fromText("hello")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {" /help", "  /help  "})
    void fromText_withWhitespace(String input) {
        assertThat(BotCommand.fromText(input)).contains(BotCommand.HELP);
    }

    @Test
    void commandAndDescription_arePopulated() {
        assertThat(BotCommand.START.getCommand()).isEqualTo("/start");
        assertThat(BotCommand.START.getDescription()).isNotBlank();
    }
}
