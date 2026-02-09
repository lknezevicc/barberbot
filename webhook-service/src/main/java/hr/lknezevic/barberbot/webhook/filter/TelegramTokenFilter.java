package hr.lknezevic.barberbot.webhook.filter;

import hr.lknezevic.barberbot.config.application.ApplicationConfig;
import hr.lknezevic.barberbot.config.application.TelegramConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public final class TelegramTokenFilter extends OncePerRequestFilter {

    private final ApplicationConfig applicationConfig;
    private final TelegramConfig telegramConfig;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/telegram/webhook")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!applicationConfig.enabled()) {
            log.warn("Telegram Webhook API is disabled");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        final String token = request.getHeader(telegramConfig.secretTokenHeader());
        if (!telegramConfig.secretToken().equals(token)) {
            log.warn("Invalid Telegram Webhook Token for {}: {}", request.getRequestURI(), request);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
