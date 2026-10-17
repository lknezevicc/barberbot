package hr.lknezevic.barberbot.admin.controller;

import hr.lknezevic.barberbot.admin.config.JwtTokenProvider;
import hr.lknezevic.barberbot.admin.entity.AppUser;
import hr.lknezevic.barberbot.admin.entity.Tenant;
import hr.lknezevic.barberbot.admin.repository.AppUserRepository;
import hr.lknezevic.barberbot.admin.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AppUserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthController authController;

    private final UUID userId = UUID.randomUUID();
    private final UUID tenantId = UUID.randomUUID();

    private AppUser adminUser() {
        Tenant tenant = Tenant.builder().id(tenantId).name("TestSalon").build();
        return AppUser.builder()
                .id(userId)
                .tenant(tenant)
                .email("admin@test.com")
                .passwordHash("$2a$hashed")
                .firstName("Admin")
                .role("ADMIN")
                .build();
    }

    @Test
    void login_validCredentials_returnsToken() {
        AppUser user = adminUser();
        when(userRepository.findByEmailWithTenant("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
        when(jwtTokenProvider.generateToken(userId, tenantId, "ADMIN")).thenReturn("jwt-token-here");

        AuthController.LoginRequest request = new AuthController.LoginRequest("admin@test.com", "password123");
        ResponseEntity<Map<String, String>> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("token")).isEqualTo("jwt-token-here");
        assertThat(response.getBody().get("role")).isEqualTo("ADMIN");
    }

    @Test
    void login_wrongPassword_returns401() {
        AppUser user = adminUser();
        when(userRepository.findByEmailWithTenant("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "$2a$hashed")).thenReturn(false);

        AuthController.LoginRequest request = new AuthController.LoginRequest("admin@test.com", "wrongpass");
        ResponseEntity<Map<String, String>> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody()).containsKey("error");
    }

    @Test
    void login_nonexistentUser_returns401() {
        when(userRepository.findByEmailWithTenant("nobody@test.com")).thenReturn(Optional.empty());

        AuthController.LoginRequest request = new AuthController.LoginRequest("nobody@test.com", "password");
        ResponseEntity<Map<String, String>> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void login_clientRole_returns401() {
        AppUser clientUser = adminUser();
        clientUser.setRole("CLIENT");
        when(userRepository.findByEmailWithTenant("admin@test.com")).thenReturn(Optional.of(clientUser));
        when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);

        AuthController.LoginRequest request = new AuthController.LoginRequest("admin@test.com", "password123");
        ResponseEntity<Map<String, String>> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void login_noPasswordHash_returns401() {
        AppUser user = adminUser();
        user.setPasswordHash(null);
        when(userRepository.findByEmailWithTenant("admin@test.com")).thenReturn(Optional.of(user));

        AuthController.LoginRequest request = new AuthController.LoginRequest("admin@test.com", "password");
        ResponseEntity<Map<String, String>> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void login_superadminRole_succeeds() {
        AppUser user = adminUser();
        user.setRole("SUPERADMIN");
        when(userRepository.findByEmailWithTenant("admin@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "$2a$hashed")).thenReturn(true);
        when(jwtTokenProvider.generateToken(userId, tenantId, "SUPERADMIN")).thenReturn("jwt-super");

        AuthController.LoginRequest request = new AuthController.LoginRequest("admin@test.com", "password123");
        ResponseEntity<Map<String, String>> response = authController.login(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody().get("role")).isEqualTo("SUPERADMIN");
    }
}
