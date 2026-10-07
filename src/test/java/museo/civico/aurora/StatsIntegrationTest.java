package museo.civico.aurora;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.Booking;
import museo.civico.aurora.domain.BookingStatus;
import museo.civico.aurora.domain.CulturalEvent;
import museo.civico.aurora.domain.EventStatus;
import museo.civico.aurora.domain.Feedback;
import museo.civico.aurora.support.AbstractIntegrationTest;

/**
 * T10: ogni metrica e il denominatore corrispondono ai valori fixture noti,
 * incluso il caso di capienza totale zero.
 */
class StatsIntegrationTest extends AbstractIntegrationTest {

	@Test
	void statsMatchKnownFixture() throws Exception {
		AppUser admin = createAdmin("stats-admin@example.com");

		CulturalEvent future1 = persistEvent("Futuro 1", EventStatus.PUBLISHED, 10, FUTURE_START, FUTURE_END);
		CulturalEvent future2 = persistEvent("Futuro 2", EventStatus.PUBLISHED, 10, FUTURE_START.plusDays(1),
				FUTURE_END.plusDays(1));
		CulturalEvent past = persistEvent("Passato", EventStatus.PUBLISHED, 100, PAST_START, PAST_END);
		persistEvent("Bozza", EventStatus.DRAFT, 50, FUTURE_START, FUTURE_END);
		persistEvent("Annullato", EventStatus.CANCELLED, 50, FUTURE_START, FUTURE_END);

		AppUser v1 = createVisitor("s1@example.com");
		AppUser v2 = createVisitor("s2@example.com");
		AppUser v3 = createVisitor("s3@example.com");
		AppUser v4 = createVisitor("s4@example.com");
		AppUser v5 = createVisitor("s5@example.com");
		AppUser v6 = createVisitor("s6@example.com");
		AppUser v7 = createVisitor("s7@example.com");
		AppUser v8 = createVisitor("s8@example.com");
		AppUser v9 = createVisitor("s9@example.com");

		// 3 confermate su futuro1, 1 su futuro2, 5 sul passato (fuori dal conteggio atteso).
		bookingRepository.saveAndFlush(new Booking(future1, v1, BookingStatus.CONFIRMED, NOW));
		bookingRepository.saveAndFlush(new Booking(future1, v2, BookingStatus.CONFIRMED, NOW));
		bookingRepository.saveAndFlush(new Booking(future1, v3, BookingStatus.CONFIRMED, NOW));
		bookingRepository.saveAndFlush(new Booking(future2, v4, BookingStatus.CONFIRMED, NOW));
		bookingRepository.saveAndFlush(new Booking(past, v5, BookingStatus.CONFIRMED, PAST_START));
		bookingRepository.saveAndFlush(new Booking(past, v6, BookingStatus.CONFIRMED, PAST_START));
		bookingRepository.saveAndFlush(new Booking(past, v7, BookingStatus.CONFIRMED, PAST_START));
		bookingRepository.saveAndFlush(new Booking(past, v8, BookingStatus.CONFIRMED, PAST_START));
		bookingRepository.saveAndFlush(new Booking(past, v9, BookingStatus.CONFIRMED, PAST_START));
		// Una prenotazione annullata non conta.
		Booking cancelled = new Booking(future1, v5, BookingStatus.CANCELLED, NOW);
		cancelled.setCancelledAt(NOW);
		bookingRepository.saveAndFlush(cancelled);

		feedbackRepository.saveAndFlush(new Feedback(future1, v1, 5, "ottimo", NOW));
		feedbackRepository.saveAndFlush(new Feedback(future2, v2, 4, "buono", NOW));

		mockMvc.perform(get("/api/admin/stats").with(user(principalOf(admin))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publishedEventCount").value(3))
				.andExpect(jsonPath("$.upcomingEventCount").value(2))
				.andExpect(jsonPath("$.confirmedBookingCount").value(4))
				.andExpect(jsonPath("$.capacityUtilizationPercent").value(20.0))
				.andExpect(jsonPath("$.averageRating").value(4.5))
				.andExpect(jsonPath("$.feedbackCount").value(2));
	}

	@Test
	void statsHandleZeroCapacityDenominator() throws Exception {
		AppUser admin = createAdmin("zero-admin@example.com");

		mockMvc.perform(get("/api/admin/stats").with(user(principalOf(admin))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publishedEventCount").value(0))
				.andExpect(jsonPath("$.upcomingEventCount").value(0))
				.andExpect(jsonPath("$.confirmedBookingCount").value(0))
				.andExpect(jsonPath("$.capacityUtilizationPercent").value(0.0))
				.andExpect(jsonPath("$.averageRating").value(nullValue()))
				.andExpect(jsonPath("$.feedbackCount").value(0));
	}
}
