package museo.civico.aurora.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import museo.civico.aurora.domain.BookingStatus;
import museo.civico.aurora.domain.EventStatus;
import museo.civico.aurora.dto.AdminStatsDto;
import museo.civico.aurora.repository.BookingRepository;
import museo.civico.aurora.repository.CulturalEventRepository;
import museo.civico.aurora.repository.FeedbackRepository;

/**
 * Indicatori aggregati secondo le formule del contratto.
 */
@Service
public class StatsService {

	private final CulturalEventRepository events;
	private final BookingRepository bookings;
	private final FeedbackRepository feedbacks;
	private final Clock clock;

	public StatsService(CulturalEventRepository events, BookingRepository bookings, FeedbackRepository feedbacks,
			Clock clock) {
		this.events = events;
		this.bookings = bookings;
		this.feedbacks = feedbacks;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public AdminStatsDto getStats() {
		OffsetDateTime now = OffsetDateTime.now(clock);

		long publishedEventCount = events.countByStatus(EventStatus.PUBLISHED);
		long upcomingEventCount = events.countByStatusAndStartsAtGreaterThanEqual(EventStatus.PUBLISHED, now);
		long confirmedBookingCount = bookings.countConfirmedForUpcomingPublished(BookingStatus.CONFIRMED,
				EventStatus.PUBLISHED, now);
		long totalCapacity = events.sumCapacityByStatusAndStartsAtGreaterThanEqual(EventStatus.PUBLISHED, now);

		double utilization = totalCapacity == 0 ? 0.0
				: (100.0 * confirmedBookingCount) / totalCapacity;

		Double rawAverage = feedbacks.averageRating();
		Double averageRating = rawAverage == null ? null
				: BigDecimal.valueOf(rawAverage).setScale(2, RoundingMode.HALF_UP).doubleValue();

		return new AdminStatsDto(publishedEventCount, upcomingEventCount, confirmedBookingCount, utilization,
				averageRating, feedbacks.count());
	}
}
