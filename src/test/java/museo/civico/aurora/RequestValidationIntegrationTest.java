package museo.civico.aurora;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.CulturalEvent;
import museo.civico.aurora.domain.EventStatus;
import museo.civico.aurora.support.AbstractIntegrationTest;

/**
 * T12: paginazione e intervallo temporale invalidi -> 400; media type errato ->
 * 415; pagina oltre l'ultima -> 200 con content vuoto.
 */
class RequestValidationIntegrationTest extends AbstractIntegrationTest {

	@Test
	void invalidPageAndSizeReturn400() throws Exception {
		mockMvc.perform(get("/api/events").param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PAGE"));

		mockMvc.perform(get("/api/events").param("size", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));

		mockMvc.perform(get("/api/events").param("size", "51"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
	}

	@Test
	void invalidTimeRangeReturns400() throws Exception {
		mockMvc.perform(get("/api/events")
						.param("from", "2026-07-01T00:00:00Z")
						.param("to", "2026-06-01T00:00:00Z"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
	}

	@Test
	void pageBeyondLastReturnsEmpty200() throws Exception {
		persistPublishedFutureEvent("Unico", 5);

		mockMvc.perform(get("/api/events").param("page", "999").param("size", "20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(0))
				.andExpect(jsonPath("$.page").value(999));
	}

	@Test
	void sizeAtMaximumIsAccepted() throws Exception {
		persistPublishedFutureEvent("Dimensione massima", 5);

		mockMvc.perform(get("/api/events").param("size", "50"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(50))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void temporalFilterFromAndToAreInclusive() throws Exception {
		persistEvent("Boundary", EventStatus.PUBLISHED, 5, FUTURE_START, FUTURE_END);
		persistEvent("Later", EventStatus.PUBLISHED, 5, FUTURE_START.plusDays(2), FUTURE_END.plusDays(2));

		String boundary = FUTURE_START.toString();
		mockMvc.perform(get("/api/events").param("from", boundary).param("to", boundary))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].title").value("Boundary"));
	}

	@Test
	void unsupportedMediaTypeReturns415() throws Exception {
		mockMvc.perform(post("/api/auth/register").with(csrfToken())
						.contentType(MediaType.TEXT_PLAIN)
						.content("not-json"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	void authenticatedPaginationInvalidReturns400() throws Exception {
		AppUser visitor = createVisitor("paging@example.com");

		mockMvc.perform(get("/api/bookings/me").with(user(principalOf(visitor))).param("page", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PAGE"));

		mockMvc.perform(get("/api/bookings/me").with(user(principalOf(visitor))).param("size", "100"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PAGE_SIZE"));
	}

	@Test
	void bookingWithoutValidEventIdReturns400() throws Exception {
		AppUser visitor = createVisitor("invalid-booking@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Evento", 5);
		// Sanity: l'evento esiste, ma il body non e' valido.
		mockMvc.perform(post("/api/bookings").with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(get("/api/events/" + event.getId()))
				.andExpect(status().isOk());
	}
}
