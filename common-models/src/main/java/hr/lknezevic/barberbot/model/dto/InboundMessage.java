package hr.lknezevic.barberbot.model.dto;

import hr.lknezevic.barberbot.model.enums.MessageType;

public record InboundMessage(
        String tenantId,
        Long chatId,
        String userId,
        String text,
        String callbackData,
        MessageType messageType
) {
}
