package hr.lknezevic.barberbot.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantContextService {

    private static final long CACHE_TTL_MS = 5 * 60 * 1000;

    private final JdbcTemplate jdbcTemplate;
    private final Map<String, CachedContext> cache = new ConcurrentHashMap<>();

    public TenantContext getContext(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) return TenantContext.EMPTY;

        CachedContext cached = cache.get(tenantId);
        if (cached != null && !cached.isExpired()) {
            return cached.context;
        }

        try {
            TenantContext ctx = loadFromDatabase(UUID.fromString(tenantId));
            cache.put(tenantId, new CachedContext(ctx));
            return ctx;
        } catch (Exception e) {
            log.warn("Failed to load tenant context for {}: {}", tenantId, e.getMessage());
            return TenantContext.EMPTY;
        }
    }

    private TenantContext loadFromDatabase(UUID tenantId) {
        List<EmployeeInfo> employees = jdbcTemplate.query(
                "SELECT name, specialization FROM employees WHERE tenant_id = ? AND active = true ORDER BY name",
                (rs, i) -> new EmployeeInfo(rs.getString("name"), rs.getString("specialization")),
                tenantId);

        List<ServiceInfo> services = jdbcTemplate.query(
                "SELECT name, duration_minutes, price FROM services WHERE tenant_id = ? AND active = true ORDER BY name",
                (rs, i) -> new ServiceInfo(rs.getString("name"), rs.getInt("duration_minutes"),
                        rs.getBigDecimal("price").doubleValue()),
                tenantId);

        List<WorkingHoursInfo> hours = jdbcTemplate.query(
                "SELECT e.name AS employee_name, wh.day_of_week, wh.start_time, wh.end_time " +
                "FROM working_hours wh JOIN employees e ON e.id = wh.employee_id " +
                "WHERE wh.tenant_id = ? ORDER BY e.name, wh.day_of_week",
                (rs, i) -> new WorkingHoursInfo(
                        rs.getString("employee_name"),
                        rs.getInt("day_of_week"),
                        rs.getString("start_time").substring(0, 5),
                        rs.getString("end_time").substring(0, 5)),
                tenantId);

        log.debug("Loaded tenant context: {} employees, {} services, {} working hours entries",
                employees.size(), services.size(), hours.size());

        return new TenantContext(employees, services, hours);
    }

    public record EmployeeInfo(String name, String specialization) {}
    public record ServiceInfo(String name, int durationMinutes, double price) {}
    public record WorkingHoursInfo(String employeeName, int dayOfWeek, String startTime, String endTime) {
        public String dayName() {
            return switch (dayOfWeek) {
                case 1 -> "ponedjeljak";
                case 2 -> "utorak";
                case 3 -> "srijeda";
                case 4 -> "cetvrtak";
                case 5 -> "petak";
                case 6 -> "subota";
                case 7 -> "nedjelja";
                default -> "dan " + dayOfWeek;
            };
        }
    }

    public record TenantContext(
            List<EmployeeInfo> employees,
            List<ServiceInfo> services,
            List<WorkingHoursInfo> workingHours
    ) {
        static final TenantContext EMPTY = new TenantContext(List.of(), List.of(), List.of());

        public boolean isEmpty() {
            return employees.isEmpty() && services.isEmpty();
        }

        public String toPromptBlock() {
            if (isEmpty()) return "";

            StringBuilder sb = new StringBuilder();
            sb.append("PODACI O SALONU:\n\n");

            if (!employees.isEmpty()) {
                sb.append("FRIZERI (koristi TOČNO ova imena):\n");
                for (EmployeeInfo e : employees) {
                    sb.append("- ").append(e.name());
                    if (e.specialization() != null && !e.specialization().isBlank()) {
                        sb.append(" (").append(e.specialization()).append(")");
                    }
                    sb.append("\n");
                }
                sb.append("\n");
            }

            if (!services.isEmpty()) {
                sb.append("USLUGE (koristi TOČNO ova imena):\n");
                for (ServiceInfo s : services) {
                    sb.append("- ").append(s.name())
                      .append(" (").append(s.durationMinutes()).append(" min, ")
                      .append(String.format("%.0f", s.price())).append(" EUR)\n");
                }
                sb.append("\n");
            }

            if (!workingHours.isEmpty()) {
                sb.append("RADNO VRIJEME:\n");
                String currentEmployee = "";
                for (WorkingHoursInfo wh : workingHours) {
                    if (!wh.employeeName().equals(currentEmployee)) {
                        currentEmployee = wh.employeeName();
                        sb.append("  ").append(currentEmployee).append(":\n");
                    }
                    sb.append("    ").append(wh.dayName()).append(": ")
                      .append(wh.startTime()).append(" - ").append(wh.endTime()).append("\n");
                }
                sb.append("\n");
            }

            sb.append("""
                    PRAVILA ZA KORIŠTENJE PODATAKA:
                    - Kada korisnik pita za usluge, navedi TOČNA imena usluga iz gornjeg popisa
                    - Kada korisnik pita za frizere, navedi TOČNA imena iz gornjeg popisa
                    - U bookingDraft UVIJEK koristi puno ime frizera (ime i prezime) iz popisa
                    - U bookingDraft UVIJEK koristi točno ime usluge iz popisa (npr. "Sisanje", ne "šišanje")
                    - Ako korisnik kaže samo ime (npr. "Marko"), pronađi puno ime iz popisa (npr. "Marko Horvat")
                    - Ako frizera nema u popisu, postavi barber na null i pitaj korisnika
                    - Ako uslugu nema u popisu, postavi service na null i pitaj korisnika
                    - Ne izmišljaj frizere ili usluge koje nisu u popisu
                    """);

            return sb.toString();
        }
    }

    private record CachedContext(TenantContext context, long timestamp) {
        CachedContext(TenantContext context) {
            this(context, System.currentTimeMillis());
        }
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_TTL_MS;
        }
    }
}
