package hr.lknezevic.barberbot.admin.entity;

import hr.lknezevic.barberbot.model.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceEntity serviceEntity;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long chatId;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    @Column(nullable = false)
    private String barber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Builder.Default
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 30;

    @Column(name = "held_at")
    private Instant heldAt;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "customer_phone", length = 64)
    private String customerPhone;

    @Column(nullable = false)
    private Instant updatedAt;

    @JsonProperty("tenantId")
    public UUID getTenantIdFk() {
        return tenant != null ? tenant.getId() : null;
    }

    @JsonProperty("employeeId")
    public UUID getEmployeeIdFk() {
        return employee != null ? employee.getId() : null;
    }

    @JsonProperty("serviceId")
    public UUID getServiceIdFk() {
        return serviceEntity != null ? serviceEntity.getId() : null;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = BookingStatus.CONFIRMED;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
