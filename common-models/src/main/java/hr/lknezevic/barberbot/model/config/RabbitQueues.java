package hr.lknezevic.barberbot.model.config;

public final class RabbitQueues {

    private RabbitQueues() {}

    public static final String EXCHANGE = "barberbot.exchange";
    public static final String UPDATE_INBOUND_QUEUE = "telegram.update.inbound";
    public static final String UPDATE_INBOUND_ROUTING_KEY = "telegram.update.inbound";
    public static final String REPLY_OUTBOUND_QUEUE = "telegram.reply.outbound";
    public static final String REPLY_OUTBOUND_ROUTING_KEY = "telegram.reply.outbound";
}
