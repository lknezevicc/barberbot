package hr.lknezevic.barberbot.bot.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.enums.MessageType;
import hr.lknezevic.barberbot.model.event.TelegramUpdateEvent;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateEventParserTest {

    private final UpdateEventParser parser = new UpdateEventParser(new ObjectMapper());

    @Test
    void parsesTextMessage() {
        String json = """
                {"message":{"chat":{"id":12345},"from":{"id":99,"first_name":"Leon","last_name":"K"},"text":"Trebam šišanje"}}""";
        TelegramUpdateEvent event = TelegramUpdateEvent.of("tenant-1", json);

        Optional<InboundMessage> result = parser.parse(event);

        assertThat(result).isPresent();
        InboundMessage msg = result.get();
        assertThat(msg.chatId()).isEqualTo(12345L);
        assertThat(msg.userId()).isEqualTo("99");
        assertThat(msg.text()).isEqualTo("Trebam šišanje");
        assertThat(msg.messageType()).isEqualTo(MessageType.TEXT);
        assertThat(msg.customerName()).isEqualTo("Leon K");
        assertThat(msg.tenantId()).isEqualTo("tenant-1");
    }

    @Test
    void parsesCommandMessage() {
        String json = """
                {"message":{"chat":{"id":100},"from":{"id":42,"first_name":"Ana"},"text":"/help"}}""";
        TelegramUpdateEvent event = TelegramUpdateEvent.of("t1", json);

        Optional<InboundMessage> result = parser.parse(event);

        assertThat(result).isPresent();
        assertThat(result.get().messageType()).isEqualTo(MessageType.COMMAND);
        assertThat(result.get().text()).isEqualTo("/help");
    }

    @Test
    void parsesCallbackQuery() {
        String json = """
                {"callback_query":{"message":{"chat":{"id":555}},"from":{"id":77,"first_name":"Marko","last_name":"H"},"data":"hold:confirm:abc"}}""";
        TelegramUpdateEvent event = TelegramUpdateEvent.of("t1", json);

        Optional<InboundMessage> result = parser.parse(event);

        assertThat(result).isPresent();
        InboundMessage msg = result.get();
        assertThat(msg.chatId()).isEqualTo(555L);
        assertThat(msg.userId()).isEqualTo("77");
        assertThat(msg.callbackData()).isEqualTo("hold:confirm:abc");
        assertThat(msg.messageType()).isEqualTo(MessageType.CALLBACK);
        assertThat(msg.customerName()).isEqualTo("Marko H");
    }

    @Test
    void extractsCustomerName_firstNameOnly() {
        String json = """
                {"message":{"chat":{"id":1},"from":{"id":1,"first_name":"Leon"},"text":"bok"}}""";
        TelegramUpdateEvent event = TelegramUpdateEvent.of("t1", json);

        Optional<InboundMessage> result = parser.parse(event);

        assertThat(result).isPresent();
        assertThat(result.get().customerName()).isEqualTo("Leon");
    }

    @Test
    void messageWithoutText_returnsEmpty() {
        String json = """
                {"message":{"chat":{"id":1},"from":{"id":1,"first_name":"Leon"}}}""";
        TelegramUpdateEvent event = TelegramUpdateEvent.of("t1", json);

        assertThat(parser.parse(event)).isEmpty();
    }

    @Test
    void unsupportedUpdateType_returnsEmpty() {
        String json = """
                {"edited_message":{"chat":{"id":1}}}""";
        TelegramUpdateEvent event = TelegramUpdateEvent.of("t1", json);

        assertThat(parser.parse(event)).isEmpty();
    }

    @Test
    void invalidJson_returnsEmpty() {
        TelegramUpdateEvent event = TelegramUpdateEvent.of("t1", "not json");
        assertThat(parser.parse(event)).isEmpty();
    }
}
