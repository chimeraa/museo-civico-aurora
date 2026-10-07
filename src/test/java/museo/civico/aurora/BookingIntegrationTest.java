package museo.civico.aurora;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.Booking;
import museo.civico.aurora.domain.BookingStatus;
import museo.civico.aurora.domain.CulturalEvent;
import museo.civico.aurora.domain.EventStatus;
import museo.civico.aurora.support.AbstractIntegrationTest;
import museo.civico.aurora.support.JsonTestSupport;

/**
 * T2 (doppia prenotazione attiva), T3 (annullamento e nuova prenotazione),
 * regole di capienza, proprietario e stato.
 */
class BookingIntegrationTest extends AbstractIntegrationTest {

	@Test
	void duplicateActiveBookingReturnsAlreadyBooked() throws Exception {
		AppUser visitor = createVisitor("dup@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Concorrenza", 5);

		mockMvc.perform(post("/api/bookings").with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":" + event.getId() + "}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CONFIRMED"));

		mockMvc.perform(post("/api/bookings").with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":" + event.getId() + "}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_BOOKED"));

		assertThat(bookingRepository.countByEventIdAndStatus(event.getId(), BookingStatus.CONFIRMED)).isEqualTo(1);
	}

	@Test
	void cancelFreesSeatAndAllowsRebook() throws Exception {
		AppUser visitor = createVisitor("cancel@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Annullabile", 1);

		String created = mockMvc.perform(post("/api/bookings").with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":" + event.getId() + "}"))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		long bookingId = JsonTestSupport.extractLong(created, "id");

		// Capienza piena: il posto e' occupato.
		AppUser other = createVisitor("other@example.com");
		mockMvc.perform(post("/api/bookings").with(user(principalOf(other))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":" + event.getId() + "}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EVENT_FULL"));

		mockMvc.perform(delete("/api/bookings/" + bookingId).with(user(principalOf(visitor))).with(csrfToken()))
				.andExpect(status().isNoContent());

		// Idempotente.
		mockMvc.perform(delete("/api/bookings/" + bookingId).with(user(principalOf(visitor))).with(csrfToken()))
				.andExpect(status().isNoContent());

		// L'utente puo' riprenotare dopo l'annullamento.
		mockMvc.perform(post("/api/bookings").with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":" + event.getId() + "}"))
				.andExpect(status().isCreated());
	}

	@Test
	void cancellingStartedBookingIsRejected() throws Exception {
		AppUser visitor = createVisitor("started@example.com");
		CulturalEvent event = persistEvent("Iniziato", EventStatus.PUBLISHED, 5, PAST_START, PAST_END);
		Booking booking = bookingRepository
				.saveAndFlush(new Booking(event, visitor, BookingStatus.CONFIRMED, PAST_START));

		mockMvc.perform(delete("/api/bookings/" + booking.getId()).with(user(principalOf(visitor))).with(csrfToken()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BOOKING_ALREADY_STARTED"));
	}

	@Test
	void cancellingAlreadyCancelledStartedBookingIsIdempotent() throws Exception {
		AppUser visitor = createVisitor("started2@example.com");
		CulturalEvent event = persistEvent("Iniziato", EventStatus.PUBLISHED, 5, PAST_START, PAST_END);
		Booking booking = new Booking(event, visitor, BookingStatus.CANCELLED, PAST_START);
		booking.setCancelledAt(PAST_START);
		booking = bookingRepository.saveAndFlush(booking);

		mockMvc.perform(delete("/api/bookings/" + booking.getId()).with(user(principalOf(visitor))).with(csrfToken()))
				.andExpect(status().isNoContent());
	}

	@Test
	void cancellingForeignBookingReturnsNotFound() throws Exception {
		AppUser owner = createVisitor("owner@example.com");
		AppUser intruder = createVisitor("intruder@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Altrui", 5);
		Booking booking = bookingRepository
				.saveAndFlush(new Booking(event, owner, BookingStatus.CONFIRMED, NOW));

		mockMvc.perform(delete("/api/bookings/" + booking.getId()).with(user(principalOf(intruder))).with(csrfToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void bookingMissingOrUnpublishedEventReturnsNotFound() throws Exception {
		AppUser visitor = createVisitor("missing@example.com");
		persistEvent("Bozza", EventStatus.DRAFT, 5, FUTURE_START, FUTURE_END);

		mockMvc.perform(post("/api/bookings").with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":9999}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));

		mockMvc.perform(post("/api/bookings").with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":1}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void bookingStartedEventReturnsEventAlreadyStarted() throws Exception {
		AppUser visitor = createVisitor("late@example.com");
		CulturalEvent event = persistEvent("Iniziato", EventStatus.PUBLISHED, 5, PAST_START, PAST_END);

		mockMvc.perform(post("/api/bookings").with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":" + event.getId() + "}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("EVENT_ALREADY_STARTED"));
	}

	@Test
	void myBookingsListsOwnBookings() throws Exception {
		AppUser visitor = createVisitor("mine@example.com");
		AppUser other = createVisitor("other2@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Mie", 5);
		bookingRepository.saveAndFlush(new Booking(event, visitor, BookingStatus.CONFIRMED, NOW));
		bookingRepository.saveAndFlush(new Booking(event, other, BookingStatus.CONFIRMED, NOW));

		mockMvc.perform(get("/api/bookings/me").with(user(principalOf(visitor))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].eventTitle").value("Mie"));
	}

	@Test
	void adminCanCancelBookingAndRepeatedCancelIsIdempotent() throws Exception {
		AppUser admin = createAdmin("admin-cancel@example.com");
		AppUser visitor = createVisitor("admin-cancel-visitor@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Annullamento admin", 5);
		Booking booking = bookingRepository
				.saveAndFlush(new Booking(event, visitor, BookingStatus.CONFIRMED, NOW));

		mockMvc.perform(post("/api/admin/bookings/" + booking.getId() + "/cancel")
						.with(user(principalOf(admin))).with(csrfToken()))
				.andExpect(status().isNoContent());

		assertThat(bookingRepository.findById(booking.getId()).orElseThrow().getStatus())
				.isEqualTo(BookingStatus.CANCELLED);

		mockMvc.perform(post("/api/admin/bookings/" + booking.getId() + "/cancel")
						.with(user(principalOf(admin))).with(csrfToken()))
				.andExpect(status().isNoContent());
	}

	@Test
	void adminCancelMissingBookingReturns404() throws Exception {
		AppUser admin = createAdmin("admin-missing@example.com");

		mockMvc.perform(post("/api/admin/bookings/999999/cancel")
						.with(user(principalOf(admin))).with(csrfToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void adminCancelStartedBookingReturnsBookingAlreadyStarted() throws Exception {
		AppUser admin = createAdmin("admin-started@example.com");
		AppUser visitor = createVisitor("admin-started-visitor@example.com");
		CulturalEvent event = persistEvent("Iniziato", EventStatus.PUBLISHED, 5, PAST_START, PAST_END);
		Booking booking = bookingRepository
				.saveAndFlush(new Booking(event, visitor, BookingStatus.CONFIRMED, PAST_START));

		mockMvc.perform(post("/api/admin/bookings/" + booking.getId() + "/cancel")
						.with(user(principalOf(admin))).with(csrfToken()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BOOKING_ALREADY_STARTED"));
	}

	@Test
	void adminBookingsListShowsVisitorNameWithoutEmail() throws Exception {
		AppUser admin = createAdmin("admin-list@example.com");
		AppUser visitor = createVisitor("admin-list-visitor@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Elenco admin", 5);
		bookingRepository.saveAndFlush(new Booking(event, visitor, BookingStatus.CONFIRMED, NOW));

		mockMvc.perform(get("/api/admin/events/" + event.getId() + "/bookings")
						.with(user(principalOf(admin))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].visitorDisplayName").value("Visitatore"))
				.andExpect(jsonPath("$.content[0].email").doesNotExist());
	}
}
