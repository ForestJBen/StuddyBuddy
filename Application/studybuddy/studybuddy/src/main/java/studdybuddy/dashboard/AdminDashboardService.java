package studdybuddy.dashboard;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import studdybuddy.dashboard.dto.AdminDashboardResponse;
import studdybuddy.dashboard.dto.AdminUserResponse;
import studdybuddy.user.UserRepository;
import studdybuddy.user.UserRole;

@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final UserRepository userRepository;

    public AdminDashboardService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AdminDashboardResponse getDashboard() {
        return new AdminDashboardResponse(
                userRepository.count(),
                userRepository.countByRole(UserRole.STUDENT),
                userRepository.countByRole(UserRole.ADMIN)
        );
    }

    public Page<AdminUserResponse> getUsers(Pageable pageable) {
        return userRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(AdminUserResponse::from);
    }
}
