package museo.civico.aurora.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import museo.civico.aurora.domain.Feedback;

// Accesso al database per i giudizi.
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

	Page<Feedback> findByEventId(Long eventId, Pageable pageable);

	// Voto medio di tutti i giudizi. Vale null se non ce ne sono.
	@Query("select avg(f.rating) from Feedback f")
	Double averageRating();
}
