package hr.lknezevic.barberbot.model;

import org.telegram.telegrambots.meta.api.objects.Update;

public record LockedUpdate(
        Update update,
        String token,
        String userId
) { }
