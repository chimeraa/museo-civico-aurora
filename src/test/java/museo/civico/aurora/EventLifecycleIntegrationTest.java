package museo.civico.aurora;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

// Test sul ciclo di vita degli eventi: bozze, modifiche e annullamenti.
class EventLifecycleIntegrationTest extends AbstractIntegrationTest {

	// Il pubblico vede solo gli eventi pubblicati.
	@Test
	void publicListHidesDraftsAndCancelled() throws Exception {
		persistEvent("Pubblicato futuro", EventStatus.PUBLISHED, 10, FUTURE_START, FUTURE_END);
		persistEvent("Bozza", EventStatus.DRAFT, 10, FUTURE_START, FUTURE_END);
		CulturalEvent cancelled = persistEvent("Annullato", EventStatus.CANCELLED, 10, FUTURE_START, FUTURE_END);

		mockMvc.perform(get("/api/events"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].title").value("Pubblicato futuro"))
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(get("/api/events/" + cancelled.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	// Capienza zero o date al contrario danno errore 400.
	@Test
	void createEventValidatesCapacityAndDates() throws Exception {
		AppUser admin = createAdmin("admin@example.com");

		mockMvc.perform(post("/api/admin/events").with(user(principalOf(admin))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJson("Capienza zero", 0, "2026-06-10T10:00:00Z", "2026-06-10T12:00:00Z")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/admin/events").with(user(principalOf(admin))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJson("Date incoerenti", 5, "2026-06-10T12:00:00Z", "2026-06-10T10:00:00Z")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("endsAt"));
	}

	// Un evento pubblicato non può tornare bozza.
	@Test
	void publishedCannotReturnToDraft() throws Exception {
		AppUser admin = createAdmin("admin@example.com");
		CulturalEvent event = persistEvent("Pubblicato", EventStatus.PUBLISHED, 10, FUTURE_START, FUTURE_END);

		mockMvc.perform(put("/api/admin/events/" + event.getId()).with(user(principalOf(admin))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJsonWithStatus("Ritorno bozza", 10, "2026-06-10T10:00:00Z",
								"2026-06-10T12:00:00Z", "DRAFT")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_EVENT_STATE"));
	}

	// La capienza non può scendere sotto le prenotazioni confermate.
	@Test
	void capacityBelowConfirmedBookingsIsRejected() throws Exception {
		AppUser admin = createAdmin("admin@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Capienza", 5);
		AppUser v1 = createVisitor("v1@example.com");
		AppUser v2 = createVisitor("v2@example.com");
		bookingRepository.saveAndFlush(new Booking(event, v1, BookingStatus.CONFIRMED, NOW));
		bookingRepository.saveAndFlush(new Booking(event, v2, BookingStatus.CONFIRMED, NOW));

		mockMvc.perform(put("/api/admin/events/" + event.getId()).with(user(principalOf(admin))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJsonWithStatus("Capienza ridotta", 1, "2026-06-10T10:00:00Z",
								"2026-06-10T12:00:00Z", "PUBLISHED")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CAPACITY_BELOW_BOOKINGS"));
	}

	// Annullando un evento si annullano anche le sue prenotazioni.
	@Test
	void cancelEventCancelsConfirmedBookingsTransactionally() throws Exception {
		AppUser admin = createAdmin("admin@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Da annullare", 5);
		AppUser visitor = createVisitor("booker@example.com");
		Booking booking = bookingRepository
				.saveAndFlush(new Booking(event, visitor, BookingStatus.CONFIRMED, NOW));

		mockMvc.perform(post("/api/admin/events/" + event.getId() + "/cancel")
						.with(user(principalOf(admin))).with(csrfToken()))
				.andExpect(status().isNoContent());

		// Dopo l'annullamento dell'evento anche la prenotazione deve risultare annullata, con la data.
		Booking reloaded = bookingRepository.findById(booking.getId()).orElseThrow();
		assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.CANCELLED);
		assertThat(reloaded.getCancelledAt()).isNotNull();
		assertThat(eventRepository.findById(event.getId()).orElseThrow().getStatus())
				.isEqualTo(EventStatus.CANCELLED);

		mockMvc.perform(post("/api/admin/events/" + event.getId() + "/cancel")
						.with(user(principalOf(admin))).with(csrfToken()))
				.andExpect(status().isNoContent());
	}

	// Un evento già iniziato non si può annullare.
	@Test
	void cancelStartedEventIsRejected() throws Exception {
		AppUser admin = createAdmin("admin@example.com");
		CulturalEvent event = persistEvent("Già iniziato", EventStatus.PUBLISHED, 5, PAST_START, PAST_END);

		mockMvc.perform(post("/api/admin/events/" + event.getId() + "/cancel")
						.with(user(principalOf(admin))).with(csrfToken()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EVENT_CANNOT_BE_CANCELLED"));
	}

	// Un evento annullato non si può modificare.
	@Test
	void updateCancelledEventIsRejected() throws Exception {
		AppUser admin = createAdmin("admin@example.com");
		CulturalEvent event = persistEvent("Annullato", EventStatus.CANCELLED, 5, FUTURE_START, FUTURE_END);

		mockMvc.perform(put("/api/admin/events/" + event.getId()).with(user(principalOf(admin))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJsonWithStatus("Modifica annullato", 5, "2026-06-10T10:00:00Z",
								"2026-06-10T12:00:00Z", "PUBLISHED")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_EVENT_STATE"));
	}

	// Modificare un evento che non esiste dà 404, anche se i dati non vanno bene.
	@Test
	void updateUnknownEventReturns404BeforePayloadValidation() throws Exception {
		AppUser admin = createAdmin("admin-unknown@example.com");

		mockMvc.perform(put("/api/admin/events/999999").with(user(principalOf(admin))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJsonWithStatus("Inesistente", 5, "2026-06-10T10:00:00Z",
								"2026-06-10T12:00:00Z", "PUBLISHED")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	// Descrizione troppo lunga: errore 400.
	@Test
	void createEventWithTooLongDescriptionReturns400() throws Exception {
		AppUser admin = createAdmin("admin-description@example.com");
		String tooLong = "x".repeat(5001);

		mockMvc.perform(post("/api/admin/events").with(user(principalOf(admin))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content(eventJson("Descrizione lunga", 5, "2026-06-10T10:00:00Z", "2026-06-10T12:00:00Z")
								.replace("\"desc\"", "\"" + tooLong + "\"")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
	}

	// Stato che non esiste: errore 400.
	@Test
	void createEventWithUnknownStatusReturns400() throws Exception {
		AppUser admin = createAdmin("admin-status@example.com");

		mockMvc.perform(post("/api/admin/events").with(user(principalOf(admin))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Stato ignoto","description":"desc","location":"Sala","startsAt":"2026-06-10T10:00:00Z","endsAt":"2026-06-10T12:00:00Z","capacity":5,"status":"PUBLIC"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	// Crea il JSON di un evento per i test.
	private static String eventJson(String title, int capacity, String startsAt, String endsAt) {
		return """
				{"title":"%s","description":"desc","location":"Sala","startsAt":"%s","endsAt":"%s","capacity":%d}
				""".formatted(title, startsAt, endsAt, capacity);
	}

	// Come sopra, ma con anche lo stato.
	private static String eventJsonWithStatus(String title, int capacity, String startsAt, String endsAt,
			String status) {
		return """
				{"title":"%s","description":"desc","location":"Sala","startsAt":"%s","endsAt":"%s","capacity":%d,"status":"%s"}
				""".formatted(title, startsAt, endsAt, capacity, status);
	}
}
