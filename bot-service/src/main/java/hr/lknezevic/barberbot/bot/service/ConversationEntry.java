package hr.lknezevic.barberbot.bot.service;

import java.time.Instant;

public record ConversationEntry(
        String role,
        String content,
        Instant timestamp
) {

    public static ConversationEntry user(String content) {
        return new ConversationEntry("user", content, Instant.now());
    }

    public static ConversationEntry assistant(String content) {
        return new ConversationEntry("assistant", content, Instant.now());
    }
}
