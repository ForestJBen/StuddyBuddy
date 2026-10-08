package studdybuddy.admin;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studdybuddy.admin.dto.AdminDashboardSummaryResponse;
import studdybuddy.course.CourseRepository;
import studdybuddy.group.GroupMembershipRepository;
import studdybuddy.group.StudyGroupRepository;
import studdybuddy.user.UserRepository;
import studdybuddy.user.UserRole;

@Service
@Transactional(readOnly = true)
public class AdminDashboardSummaryService {

    private final UserRepository users;
    private final CourseRepository courses;
    private final StudyGroupRepository groups;
    private final GroupMembershipRepository memberships;
    private final AdminAuditRepository audits;

    public AdminDashboardSummaryService(
            UserRepository users,
            CourseRepository courses,
            StudyGroupRepository groups,
            GroupMembershipRepository memberships,
            AdminAuditRepository audits
    ) {
        this.users = users;
        this.courses = courses;
        this.groups = groups;
        this.memberships = memberships;
        this.audits = audits;
    }

    public AdminDashboardSummaryResponse getSummary() {
        return new AdminDashboardSummaryResponse(
                users.count(),
                users.countByEnabled(true),
                users.countByEnabled(false),
                users.countByRole(UserRole.STUDENT),
                users.countByRole(UserRole.ADMIN),
                courses.count(),
                groups.count(),
                memberships.count(),
                audits.count()
        );
    }
}