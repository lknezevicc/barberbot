package hr.lknezevic.barberbot.filter;

import hr.lknezevic.barberbot.config.ApplicationConfig;
import hr.lknezevic.barberbot.config.TelegramConfig;
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

        String token = request.getHeader("X-Telegram-Bot-Api-Secret-Token");
        if (!telegramConfig.secretToken().equals(token)) {
            log.warn("Invalid Telegram secret token for {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
