package hr.lknezevic.barberbot.admin.controller;

import hr.lknezevic.barberbot.admin.audit.Auditable;
import hr.lknezevic.barberbot.admin.config.JwtAuthFilter.JwtPrincipal;
import hr.lknezevic.barberbot.admin.entity.AppUser;
import hr.lknezevic.barberbot.admin.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AppUserRepository userRepository;

    @GetMapping
    public List<AppUser> list(@AuthenticationPrincipal JwtPrincipal principal) {
        return userRepository.findByTenantId(principal.tenantId());
    }

    private static final Set<String> VALID_ROLES = Set.of("CLIENT", "ADMIN");

    @PutMapping("/{id}")
    @Auditable(action = "UPDATE", entityType = "USER")
    public ResponseEntity<?> update(@PathVariable UUID id,
                                    @RequestBody UserUpdateRequest request,
                                    @AuthenticationPrincipal JwtPrincipal principal) {
        if (request.role() != null && !VALID_ROLES.contains(request.role())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role. Must be one of: " + VALID_ROLES));
        }

        return userRepository.findById(id)
                .filter(u -> u.getTenant().getId().equals(principal.tenantId()))
                .<ResponseEntity<?>>map(user -> {
                    if (request.role() != null) {
                        if ("SUPERADMIN".equals(request.role()) && !"SUPERADMIN".equals(principal.role())) {
                            return ResponseEntity.status(403).build();
                        }
                        if (id.equals(principal.userId()) && !request.role().equals(principal.role())) {
                            return ResponseEntity.status(403).build();
                        }
                        user.setRole(request.role());
                    }
                    if (request.firstName() != null) user.setFirstName(request.firstName());
                    if (request.lastName() != null) user.setLastName(request.lastName());
                    if (request.phone() != null) user.setPhone(request.phone());
                    return ResponseEntity.ok(userRepository.save(user));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Auditable(action = "DELETE", entityType = "USER")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal JwtPrincipal principal) {
        return userRepository.findById(id)
                .filter(u -> u.getTenant().getId().equals(principal.tenantId()))
                .filter(u -> !u.getId().equals(principal.userId()))
                .map(user -> {
                    userRepository.delete(user);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record UserUpdateRequest(String firstName, String lastName, String phone, String role) {}
}
