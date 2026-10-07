package museo.civico.aurora.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import museo.civico.aurora.domain.CulturalEvent;
import museo.civico.aurora.domain.EventStatus;

public interface CulturalEventRepository extends JpaRepository<CulturalEvent, Long> {

	Page<CulturalEvent> findByStatus(EventStatus status, Pageable pageable);

	Page<CulturalEvent> findByStatusAndStartsAtGreaterThanEqual(EventStatus status, OffsetDateTime from, Pageable pageable);

	Page<CulturalEvent> findByStatusAndStartsAtLessThanEqual(EventStatus status, OffsetDateTime to, Pageable pageable);

	Page<CulturalEvent> findByStatusAndStartsAtBetween(EventStatus status, OffsetDateTime from, OffsetDateTime to,
			Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from CulturalEvent e where e.id = :id")
	Optional<CulturalEvent> findByIdForUpdate(@Param("id") Long id);

	long countByStatus(EventStatus status);

	long countByStatusAndStartsAtGreaterThanEqual(EventStatus status, OffsetDateTime now);

	@Query("select coalesce(sum(e.capacity), 0) from CulturalEvent e "
			+ "where e.status = :status and e.startsAt >= :now")
	long sumCapacityByStatusAndStartsAtGreaterThanEqual(@Param("status") EventStatus status,
			@Param("now") OffsetDateTime now);
}
