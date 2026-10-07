package museo.civico.aurora.repository;

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import museo.civico.aurora.domain.Booking;
import museo.civico.aurora.domain.BookingStatus;
import museo.civico.aurora.domain.EventStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	Page<Booking> findByUserId(Long userId, Pageable pageable);

	Page<Booking> findByEventId(Long eventId, Pageable pageable);

	long countByEventIdAndStatus(Long eventId, BookingStatus status);

	@Query("select count(b) from Booking b "
			+ "where b.status = :bookingStatus and b.event.status = :eventStatus and b.event.startsAt >= :now")
	long countConfirmedForUpcomingPublished(@Param("bookingStatus") BookingStatus bookingStatus,
			@Param("eventStatus") EventStatus eventStatus, @Param("now") OffsetDateTime now);

	@Modifying
	@Query("update Booking b set b.status = :cancelled, b.cancelledAt = :now "
			+ "where b.event.id = :eventId and b.status = :confirmed")
	int cancelConfirmedByEventId(@Param("eventId") Long eventId, @Param("confirmed") BookingStatus confirmed,
			@Param("cancelled") BookingStatus cancelled, @Param("now") OffsetDateTime now);
}
