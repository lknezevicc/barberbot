package hr.lknezevic.barberbot.admin.controller;

import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.config.JwtTokenProvider;
import hr.lknezevic.barberbot.admin.entity.AppUser;
import hr.lknezevic.barberbot.admin.entity.Tenant;
import hr.lknezevic.barberbot.admin.repository.AppUserRepository;
import hr.lknezevic.barberbot.admin.repository.TenantRepository;
import hr.lknezevic.barberbot.admin.audit.Auditable;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody LoginRequest request) {
        return userRepository.findByEmailWithTenant(request.email())
                .filter(user -> user.getPasswordHash() != null)
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .filter(user -> "ADMIN".equals(user.getRole()) || "SUPERADMIN".equals(user.getRole()))
                .map(user -> {
                    String token = jwtTokenProvider.generateToken(
                            user.getId(), user.getTenant().getId(), user.getRole());
                    return ResponseEntity.ok(Map.of(
                            "token", token,
                            "role", user.getRole(),
                            "tenantId", user.getTenant().getId().toString(),
                            "userId", user.getId().toString(),
                            "name", user.getFirstName() != null ? user.getFirstName() : ""
                    ));
                })
                .orElse(ResponseEntity.status(401).body(Map.of("error", "Neispravni podaci za prijavu")));
    }

    @PostMapping("/register")
    @Auditable(action = "REGISTER", entityType = "TENANT")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        if (tenantRepository.existsByName(request.salonName())) {
            return ResponseEntity.status(409).body(Map.of("error", "Salon s tim imenom vec postoji"));
        }
        if (tenantRepository.existsByBotToken(request.botToken())) {
            return ResponseEntity.status(409).body(Map.of("error", "Bot token je vec u upotrebi"));
        }
        if (userRepository.existsByEmail(request.email())) {
            return ResponseEntity.status(409).body(Map.of("error", "Email je vec registriran"));
        }

        String generatedSecret = UUID.randomUUID().toString();

        Tenant tenant = Tenant.builder()
                .name(request.salonName())
                .botToken(request.botToken())
                .botUsername(request.botUsername())
                .secretToken(generatedSecret)
                .contactEmail(request.contactEmail())
                .address(request.address())
                .active(true)
                .build();
        tenant = tenantRepository.save(tenant);

        AppUser admin = AppUser.builder()
                .tenant(tenant)
                .telegramId(request.email())
                .chatId(0L)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role("ADMIN")
                .build();
        admin = userRepository.save(admin);

        String token = jwtTokenProvider.generateToken(admin.getId(), tenant.getId(), "ADMIN");
        return ResponseEntity.status(201).body(Map.of(
                "token", token,
                "tenantId", tenant.getId().toString(),
                "message", "Salon uspjesno registriran"
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal JwtPrincipal principal) {
        return userRepository.findByIdWithTenant(principal.userId())
                .map(user -> {
                    Map<String, Object> profile = new LinkedHashMap<>();
                    profile.put("id", user.getId().toString());
                    profile.put("email", user.getEmail() != null ? user.getEmail() : "");
                    profile.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
                    profile.put("lastName", user.getLastName() != null ? user.getLastName() : "");
                    profile.put("role", user.getRole());
                    profile.put("tenantId", user.getTenant().getId().toString());
                    profile.put("tenantName", user.getTenant().getName());
                    profile.put("tenantBotUsername", user.getTenant().getBotUsername() != null ? user.getTenant().getBotUsername() : "");
                    return ResponseEntity.ok(profile);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal JwtPrincipal principal) {
        return userRepository.findById(principal.userId())
                .filter(user -> passwordEncoder.matches(request.currentPassword(), user.getPasswordHash()))
                .map(user -> {
                    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of("message", "Lozinka uspjesno promijenjena"));
                })
                .orElse(ResponseEntity.badRequest().body(Map.of("error", "Trenutna lozinka nije ispravna")));
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank String salonName,
            @NotBlank String botToken,
            String botUsername,
            String address,
            String contactEmail,
            @NotBlank String firstName,
            String lastName,
            @NotBlank String email,
            @NotBlank @Size(min = 6) String password
    ) {}

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 6) String newPassword
    ) {}
}
