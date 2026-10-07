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
 * T9 (feedback: range del rating e unicita' per utente/evento) e visibilita'
 * del giudizio amministrativo senza email.
 */
class FeedbackIntegrationTest extends AbstractIntegrationTest {

	@Test
	void ratingOutOfRangeIsRejected() throws Exception {
		AppUser visitor = createVisitor("rating@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Feedback", 5);

		mockMvc.perform(post("/api/events/" + event.getId() + "/feedback")
						.with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\":6,\"comment\":\"troppo\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	@Test
	void commentOver2000CharsIsRejected() throws Exception {
		AppUser visitor = createVisitor("long-comment@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Feedback lungo", 5);
		String tooLongComment = "x".repeat(2001);

		mockMvc.perform(post("/api/events/" + event.getId() + "/feedback")
						.with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\":4,\"comment\":\"" + tooLongComment + "\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("comment"));
	}

	@Test
	void duplicateFeedbackForSameUserIsConflict() throws Exception {
		AppUser visitor = createVisitor("feedback@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Feedback", 5);

		mockMvc.perform(post("/api/events/" + event.getId() + "/feedback")
						.with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\":5,\"comment\":\"ottimo\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rating").value(5));

		mockMvc.perform(post("/api/events/" + event.getId() + "/feedback")
						.with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\":4,\"comment\":\"di nuovo\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("FEEDBACK_EXISTS"));
	}

	@Test
	void feedbackAllowedOnConcludedEventButNotOnDraftOrCancelled() throws Exception {
		AppUser visitor = createVisitor("concluded@example.com");
		AppUser other = createVisitor("draft@example.com");
		AppUser third = createVisitor("cancelled@example.com");
		CulturalEvent past = persistEvent("Concluso", EventStatus.PUBLISHED, 5, PAST_START, PAST_END);
		CulturalEvent draft = persistEvent("Bozza", EventStatus.DRAFT, 5, FUTURE_START, FUTURE_END);
		CulturalEvent cancelled = persistEvent("Annullato", EventStatus.CANCELLED, 5, FUTURE_START, FUTURE_END);

		mockMvc.perform(post("/api/events/" + past.getId() + "/feedback")
						.with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\":4}"))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/events/" + draft.getId() + "/feedback")
						.with(user(principalOf(other))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\":4}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/events/" + cancelled.getId() + "/feedback")
						.with(user(principalOf(third))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\":4}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void adminFeedbackListHidesEmail() throws Exception {
		AppUser visitor = createVisitor("shown@example.com");
		AppUser admin = createAdmin("admin-feedback@example.com");
		CulturalEvent event = persistPublishedFutureEvent("Feedback", 5);

		mockMvc.perform(post("/api/events/" + event.getId() + "/feedback")
						.with(user(principalOf(visitor))).with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"rating\":5,\"comment\":\"bello\"}"))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/admin/events/" + event.getId() + "/feedback")
						.with(user(principalOf(admin))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].displayName").value("Visitatore"))
				.andExpect(jsonPath("$.content[0].email").doesNotExist());
	}
}
