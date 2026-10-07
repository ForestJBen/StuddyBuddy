package studdybuddy.group;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {

    List<StudyGroup> findAllByOrderByCreatedAtDesc();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select x from StudyGroup x where x.id = :id")
    java.util.Optional<StudyGroup> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
}
