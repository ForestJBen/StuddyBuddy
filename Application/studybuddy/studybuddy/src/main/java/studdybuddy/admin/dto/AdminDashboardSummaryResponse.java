package studdybuddy.admin.dto;

public record AdminDashboardSummaryResponse(
        long totalUsers,
        long activeUsers,
        long disabledUsers,
        long studentUsers,
        long adminUsers,
        long totalCourses,
        long totalGroups,
        long totalGroupMemberships,
        long totalAuditEvents
) {
}