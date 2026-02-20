package hr.lknezevic.barberbot.model.config;

public final class RabbitQueues {

    private RabbitQueues() {}

    public static final String EXCHANGE = "barberbot.exchange";

    public static final String UPDATE_INBOUND_QUEUE = "telegram.update.inbound";
    public static final String UPDATE_INBOUND_ROUTING_KEY = "telegram.update.inbound";

    public static final String REPLY_OUTBOUND_QUEUE = "telegram.reply.outbound";
    public static final String REPLY_OUTBOUND_ROUTING_KEY = "telegram.reply.outbound";

    public static final String BOOKING_REQUEST_QUEUE = "booking.request";
    public static final String BOOKING_REQUEST_ROUTING_KEY = "booking.request";

    public static final String BOOKING_CONFIRMED_QUEUE = "booking.confirmed";
    public static final String BOOKING_CONFIRMED_ROUTING_KEY = "booking.confirmed";

    public static final String BOOKING_LIST_REQUEST_QUEUE = "booking.list.request";
    public static final String BOOKING_LIST_REQUEST_ROUTING_KEY = "booking.list.request";

    public static final String BOOKING_LIST_RESPONSE_QUEUE = "booking.list.response";
    public static final String BOOKING_LIST_RESPONSE_ROUTING_KEY = "booking.list.response";

    public static final String BOOKING_CANCEL_REQUEST_QUEUE = "booking.cancel.request";
    public static final String BOOKING_CANCEL_REQUEST_ROUTING_KEY = "booking.cancel.request";

    public static final String BOOKING_CANCELLED_QUEUE = "booking.cancelled";
    public static final String BOOKING_CANCELLED_ROUTING_KEY = "booking.cancelled";
}
