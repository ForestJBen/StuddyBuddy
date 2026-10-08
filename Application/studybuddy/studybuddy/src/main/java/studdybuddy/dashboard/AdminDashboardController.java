package studdybuddy.dashboard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import studdybuddy.dashboard.dto.AdminDashboardResponse;
import studdybuddy.dashboard.dto.AdminUserResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/dashboard")
    public AdminDashboardResponse getDashboard() {
        return adminDashboardService.getDashboard();
    }

    @GetMapping("/users")
    public Page<AdminUserResponse> getUsers(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return adminDashboardService.getUsers(pageable);
    }
}