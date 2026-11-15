package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

// Giudizio mostrato a tutti. Non contiene l'email di chi l'ha scritto.
public record FeedbackDto(Long id, Long eventId, String displayName, Integer rating, String comment,
		OffsetDateTime createdAt) {
}
