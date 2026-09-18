package studdybuddy.profile;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    @EntityGraph(attributePaths = {"user", "courses"})
    Optional<Profile> findByUser_Id(Long userId);

    @Override
    @EntityGraph(attributePaths = {"user", "courses"})
    List<Profile> findAll();
}