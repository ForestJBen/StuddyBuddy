package studdybuddy.admin;
import org.springframework.stereotype.Service;
import org.springframework.security.core.context.SecurityContextHolder;
@Service
public class AuditService {
    private final AdminAuditRepository audits;
    public AuditService(AdminAuditRepository audits) { this.audits=audits; }
    public void record(String action, String target) {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            throw new org.springframework.security.access.AccessDeniedException("Administrator access required");
        }
        audits.save(new AdminAudit(auth.getName(), action, target));
    }
}
