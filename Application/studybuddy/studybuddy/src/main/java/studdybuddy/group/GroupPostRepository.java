package studdybuddy.group;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupPostRepository extends JpaRepository<GroupPost, Long> {

    List<GroupPost> findByGroup_IdOrderByCreatedAtAsc(Long groupId);
}