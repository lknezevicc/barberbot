package hr.lknezevic.barberbot.gateway.filter;

import hr.lknezevic.barberbot.gateway.config.GatewayProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramTokenFilterTest {

    @Mock private GatewayProperties gatewayProperties;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private TelegramTokenFilter telegramTokenFilter;

    @Test
    void nonWebhookPath_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        telegramTokenFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void webhookPath_validToken_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/telegram/webhook/abc");
        request.addHeader("X-Telegram-Bot-Api-Secret-Token", "my-secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        GatewayProperties.Telegram telegram = new GatewayProperties.Telegram("my-secret");
        when(gatewayProperties.telegram()).thenReturn(telegram);

        telegramTokenFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void webhookPath_missingToken_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/telegram/webhook/abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        GatewayProperties.Telegram telegram = new GatewayProperties.Telegram("my-secret");
        when(gatewayProperties.telegram()).thenReturn(telegram);

        telegramTokenFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void webhookPath_invalidToken_returns403() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/telegram/webhook/abc");
        request.addHeader("X-Telegram-Bot-Api-Secret-Token", "wrong-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        GatewayProperties.Telegram telegram = new GatewayProperties.Telegram("my-secret");
        when(gatewayProperties.telegram()).thenReturn(telegram);

        telegramTokenFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void webhookPath_noConfiguredSecret_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/telegram/webhook/abc");
        MockHttpServletResponse response = new MockHttpServletResponse();

        GatewayProperties.Telegram telegram = new GatewayProperties.Telegram("");
        when(gatewayProperties.telegram()).thenReturn(telegram);

        telegramTokenFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
