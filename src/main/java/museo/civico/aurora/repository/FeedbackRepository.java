package museo.civico.aurora.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import museo.civico.aurora.domain.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

	Page<Feedback> findByEventId(Long eventId, Pageable pageable);

	@Query("select avg(f.rating) from Feedback f")
	Double averageRating();
}
