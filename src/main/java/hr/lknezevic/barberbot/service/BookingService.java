package hr.lknezevic.barberbot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public final class BookingService {

    private final ActionTokenService actionTokenService;
    private final BotMessageService botMessageService;

    public void sendAppointmentProposal() {

    }

}
