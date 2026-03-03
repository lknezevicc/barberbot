package hr.lknezevic.barberbot.model.event;

import java.time.LocalDate;
import java.time.LocalTime;

public record AlternativeSlot(LocalDate date, LocalTime time) {}
