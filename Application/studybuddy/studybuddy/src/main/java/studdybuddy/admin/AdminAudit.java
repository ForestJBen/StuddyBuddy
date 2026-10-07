package studdybuddy.admin;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "admin_audit")
public class AdminAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable=false) private String actor;
    @Column(nullable=false, length=50) private String action;
    @Column(nullable=false) private String target;
    @Column(name="occurred_at", nullable=false) private Instant occurredAt;
    protected AdminAudit() {}
    public AdminAudit(String actor, String action, String target) {
        this.actor=actor; this.action=action; this.target=target; this.occurredAt=Instant.now();
    }
    public Long getId() { return id; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getTarget() { return target; }
    public Instant getOccurredAt() { return occurredAt; }
}
