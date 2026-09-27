package studdybuddy.group;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {

    boolean existsByGroup_IdAndUser_Id(Long groupId, Long userId);

    Optional<GroupMembership> findByGroup_IdAndUser_Id(Long groupId, Long userId);

    long countByGroup_Id(Long groupId);

    List<GroupMembership> findByGroup_IdOrderByJoinedAtAsc(Long groupId);

    @Query("""
            SELECT membership.group.id
            FROM GroupMembership membership
            WHERE membership.user.id = :userId
            """)
    Set<Long> findGroupIdsByUserId(@Param("userId") Long userId);
}