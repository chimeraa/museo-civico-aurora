package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

/** Giudizio pubblico: non espone l'email. */
public record FeedbackDto(Long id, Long eventId, String displayName, Integer rating, String comment,
		OffsetDateTime createdAt) {
}
