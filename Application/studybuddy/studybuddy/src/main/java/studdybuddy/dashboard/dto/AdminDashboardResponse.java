package studdybuddy.dashboard.dto;

public record AdminDashboardResponse(
        long totalUsers,
        long studentUsers,
        long adminUsers
) {
}