package hr.lknezevic.barberbot.admin.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private static final String SECRET = "ThisIsAVerySecretKeyForTestingThatMustBeLongEnough64Bytes!!1234";
    private static final long EXPIRATION_MS = 3600000;

    private final JwtTokenProvider provider = new JwtTokenProvider(SECRET, EXPIRATION_MS);

    private final UUID userId = UUID.fromString("a1111111-1111-1111-1111-111111111111");
    private final UUID tenantId = UUID.fromString("b2222222-2222-2222-2222-222222222222");

    @Test
    void generatesToken_withCorrectClaims() {
        String token = provider.generateToken(userId, tenantId, "ADMIN");

        Claims claims = provider.parseToken(token);
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("tenantId", String.class)).isEqualTo(tenantId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
    }

    @Test
    void isValid_validToken_returnsTrue() {
        String token = provider.generateToken(userId, tenantId, "ADMIN");
        assertThat(provider.isValid(token)).isTrue();
    }

    @Test
    void isValid_tamperedToken_returnsFalse() {
        String token = provider.generateToken(userId, tenantId, "ADMIN");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(provider.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_expiredToken_returnsFalse() {
        JwtTokenProvider shortLived = new JwtTokenProvider(SECRET, -1000);
        String token = shortLived.generateToken(userId, tenantId, "ADMIN");
        assertThat(shortLived.isValid(token)).isFalse();
    }

    @Test
    void isValid_garbageString_returnsFalse() {
        assertThat(provider.isValid("not.a.jwt")).isFalse();
    }

    @Test
    void getUserId_extractsCorrectly() {
        String token = provider.generateToken(userId, tenantId, "ADMIN");
        assertThat(provider.getUserId(token)).isEqualTo(userId);
    }

    @Test
    void getTenantId_extractsCorrectly() {
        String token = provider.generateToken(userId, tenantId, "ADMIN");
        assertThat(provider.getTenantId(token)).isEqualTo(tenantId);
    }

    @Test
    void getRole_extractsCorrectly() {
        String token = provider.generateToken(userId, tenantId, "ADMIN");
        assertThat(provider.getRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void differentRoles_arePreserved() {
        String superAdmin = provider.generateToken(userId, tenantId, "SUPERADMIN");
        assertThat(provider.getRole(superAdmin)).isEqualTo("SUPERADMIN");
    }

    @Test
    void tokenFromDifferentSecret_isInvalid() {
        JwtTokenProvider otherProvider = new JwtTokenProvider(
                "AnotherVerySecretKeyForTestingThatIsAlsoLongEnough64Bytes!!5678", EXPIRATION_MS);
        String token = otherProvider.generateToken(userId, tenantId, "ADMIN");
        assertThat(provider.isValid(token)).isFalse();
    }
}
