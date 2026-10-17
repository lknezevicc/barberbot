package hr.lknezevic.barberbot.model.dto;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplyActionTest {

    @Test
    void of_chatIdAndText_createsMinimalReply() {
        ReplyAction action = ReplyAction.of(123L, "Hello!");

        assertThat(action.chatId()).isEqualTo(123L);
        assertThat(action.text()).isEqualTo("Hello!");
        assertThat(action.updatedSession()).isNull();
        assertThat(action.buttons()).isNull();
    }

    @Test
    void of_withSession_preservesSession() {
        Session session = new Session("s1", "u1", null, null, null, null);
        ReplyAction action = ReplyAction.of(456L, "Bok!", session);

        assertThat(action.chatId()).isEqualTo(456L);
        assertThat(action.text()).isEqualTo("Bok!");
        assertThat(action.updatedSession()).isSameAs(session);
        assertThat(action.buttons()).isNull();
    }

    @Test
    void withButtons_preservesAllFields() {
        Session session = new Session("s1", "u1", null, null, null, null);
        List<InlineButton> buttons = List.of(
                new InlineButton("Potvrdi", "hold:confirm:123"),
                new InlineButton("Odustani", "hold:cancel:123")
        );

        ReplyAction action = ReplyAction.withButtons(789L, "Potvrđuješ?", session, buttons);

        assertThat(action.chatId()).isEqualTo(789L);
        assertThat(action.text()).isEqualTo("Potvrđuješ?");
        assertThat(action.updatedSession()).isSameAs(session);
        assertThat(action.buttons()).hasSize(2);
        assertThat(action.buttons().get(0).text()).isEqualTo("Potvrdi");
        assertThat(action.buttons().get(0).callbackData()).isEqualTo("hold:confirm:123");
    }
}
