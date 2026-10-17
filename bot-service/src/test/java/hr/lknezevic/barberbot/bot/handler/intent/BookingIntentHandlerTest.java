package hr.lknezevic.barberbot.bot.handler.intent;

import hr.lknezevic.barberbot.model.config.RabbitQueues;
import hr.lknezevic.barberbot.model.dto.BookingDraft;
import hr.lknezevic.barberbot.model.dto.InboundMessage;
import hr.lknezevic.barberbot.model.dto.LlmResult;
import hr.lknezevic.barberbot.model.dto.ReplyAction;
import hr.lknezevic.barberbot.model.dto.Session;
import hr.lknezevic.barberbot.model.enums.LlmIntent;
import hr.lknezevic.barberbot.model.enums.MessageType;
import hr.lknezevic.barberbot.model.enums.SessionState;
import hr.lknezevic.barberbot.model.event.BookingRequestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class BookingIntentHandlerTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private BookingIntentHandler handler;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private Session idleSession() {
        return new Session("s1", "user-1", SessionState.IDLE, null, NOW, NOW);
    }

    private InboundMessage textMessage(String text) {
        return new InboundMessage("t1", 100L, "user-1", text, null, MessageType.TEXT, "Leon");
    }

    @Test
    void supportedIntent_isBooking() {
        assertThat(handler.supportedIntent()).isEqualTo(LlmIntent.BOOKING);
    }

    @Test
    void completeDraft_publishesBookingRequest() {
        LocalDate date = LocalDate.now().plusDays(1);
        BookingDraft draft = new BookingDraft("sisanje", date, LocalTime.of(15, 0), "Marko");
        LlmResult llmResult = new LlmResult(LlmIntent.BOOKING, draft, null, null);

        ReplyAction action = handler.handle(textMessage("sisanje sutra u 15h kod Marka"), idleSession(), llmResult);

        assertThat(action.text()).isEqualTo("Provjeravam dostupnost...");
        assertThat(action.updatedSession()).isNotNull();
        assertThat(action.updatedSession().state()).isEqualTo(SessionState.CONFIRMING);

        ArgumentCaptor<BookingRequestEvent> captor = ArgumentCaptor.forClass(BookingRequestEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitQueues.EXCHANGE), eq(RabbitQueues.BOOKING_REQUEST_ROUTING_KEY), captor.capture());

        BookingRequestEvent event = captor.getValue();
        assertThat(event.userId()).isEqualTo("user-1");
        assertThat(event.service()).isEqualTo("sisanje");
        assertThat(event.date()).isEqualTo(date);
        assertThat(event.barber()).isEqualTo("Marko");
        assertThat(event.customerName()).isEqualTo("Leon");
    }

    @Test
    void incompleteDraft_returnsReplyAskingForMissing() {
        BookingDraft draft = new BookingDraft("sisanje", null, null, null);
        LlmResult llmResult = new LlmResult(LlmIntent.BOOKING, draft, null, "Koje datum i vrijeme ti odgovara?");

        ReplyAction action = handler.handle(textMessage("trebam sisanje"), idleSession(), llmResult);

        assertThat(action.text()).isEqualTo("Koje datum i vrijeme ti odgovara?");
        assertThat(action.updatedSession().state()).isEqualTo(SessionState.BOOKING);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void incompleteDraft_generatesDefaultReply_whenLlmReplyNull() {
        BookingDraft draft = new BookingDraft("sisanje", null, null, null);
        LlmResult llmResult = new LlmResult(LlmIntent.BOOKING, draft, null, null);

        ReplyAction action = handler.handle(textMessage("sisanje"), idleSession(), llmResult);

        assertThat(action.text()).contains("date");
        assertThat(action.text()).contains("time");
        assertThat(action.text()).contains("barber");
    }

    @Test
    void mergesWithExistingDraft() {
        BookingDraft existing = new BookingDraft("sisanje", null, null, "Marko");
        Session withDraft = idleSession().withBookingDraft(existing);

        LocalDate date = LocalDate.now().plusDays(1);
        BookingDraft incoming = new BookingDraft(null, date, LocalTime.of(10, 0), null);
        LlmResult llmResult = new LlmResult(LlmIntent.BOOKING, incoming, null, null);

        ReplyAction action = handler.handle(textMessage("sutra u 10h"), withDraft, llmResult);

        assertThat(action.text()).isEqualTo("Provjeravam dostupnost...");

        ArgumentCaptor<BookingRequestEvent> captor = ArgumentCaptor.forClass(BookingRequestEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitQueues.EXCHANGE), eq(RabbitQueues.BOOKING_REQUEST_ROUTING_KEY), captor.capture());

        BookingRequestEvent event = captor.getValue();
        assertThat(event.service()).isEqualTo("sisanje");
        assertThat(event.barber()).isEqualTo("Marko");
        assertThat(event.date()).isEqualTo(date);
    }

    @Test
    void pastDate_returnsValidationError() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        BookingDraft draft = new BookingDraft("sisanje", yesterday, LocalTime.of(15, 0), "Marko");
        LlmResult llmResult = new LlmResult(LlmIntent.BOOKING, draft, null, null);

        ReplyAction action = handler.handle(textMessage("jučer u 15h"), idleSession(), llmResult);

        assertThat(action.text()).contains("prošao");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void tooFarInFuture_returnsValidationError() {
        LocalDate farFuture = LocalDate.now().plusDays(60);
        BookingDraft draft = new BookingDraft("sisanje", farFuture, LocalTime.of(15, 0), "Marko");
        LlmResult llmResult = new LlmResult(LlmIntent.BOOKING, draft, null, null);

        ReplyAction action = handler.handle(textMessage("za 2 mjeseca"), idleSession(), llmResult);

        assertThat(action.text()).contains("30 dana");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void normalizesServiceName() {
        LocalDate date = LocalDate.now().plusDays(1);
        BookingDraft draft = new BookingDraft("Haircut", date, LocalTime.of(10, 0), "Marko");
        LlmResult llmResult = new LlmResult(LlmIntent.BOOKING, draft, null, null);

        handler.handle(textMessage("haircut"), idleSession(), llmResult);

        ArgumentCaptor<BookingRequestEvent> captor = ArgumentCaptor.forClass(BookingRequestEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitQueues.EXCHANGE), eq(RabbitQueues.BOOKING_REQUEST_ROUTING_KEY), captor.capture());
        assertThat(captor.getValue().service()).isEqualTo("šišanje");
    }
}
