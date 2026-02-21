package hr.lknezevic.barberbot.model.event;

public record BookingListRequestEvent(
        String userId,
        Long chatId,
        String purpose
) {

    public static final String PURPOSE_VIEW = "VIEW";
    public static final String PURPOSE_CANCEL = "CANCEL";

    public static BookingListRequestEvent forView(String userId, Long chatId) {
        return new BookingListRequestEvent(userId, chatId, PURPOSE_VIEW);
    }

    public static BookingListRequestEvent forCancel(String userId, Long chatId) {
        return new BookingListRequestEvent(userId, chatId, PURPOSE_CANCEL);
    }
}
