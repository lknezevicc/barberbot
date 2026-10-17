package hr.lknezevic.barberbot.model.event;

public record BookingListRequestEvent(
        String userId,
        Long chatId,
        String tenantId,
        String purpose
) {

    public static final String PURPOSE_VIEW = "VIEW";
    public static final String PURPOSE_CANCEL = "CANCEL";

    public static BookingListRequestEvent forView(String userId, Long chatId, String tenantId) {
        return new BookingListRequestEvent(userId, chatId, tenantId, PURPOSE_VIEW);
    }

    public static BookingListRequestEvent forCancel(String userId, Long chatId, String tenantId) {
        return new BookingListRequestEvent(userId, chatId, tenantId, PURPOSE_CANCEL);
    }
}
