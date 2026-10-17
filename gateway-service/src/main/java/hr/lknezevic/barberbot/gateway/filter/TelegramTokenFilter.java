package hr.lknezevic.barberbot.gateway.filter;

import hr.lknezevic.barberbot.gateway.config.GatewayProperties;
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

    private final GatewayProperties gatewayProperties;

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

        String expectedToken = gatewayProperties.telegram().secretToken();
        if (expectedToken == null || expectedToken.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Telegram-Bot-Api-Secret-Token");
        if (token == null || token.isBlank()) {
            log.warn("Missing Telegram secret token for {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!expectedToken.equals(token)) {
            log.warn("Invalid Telegram secret token for {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
