package hr.lknezevic.barberbot.model.event;

public record BookingListRequestEvent(
        String userId,
        Long chatId
) {
}
