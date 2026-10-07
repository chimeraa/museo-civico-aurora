package museo.civico.aurora.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Giudizio su un evento: una valutazione 1-5 e commento facoltativo. */
public record FeedbackCreateRequest(
		@NotNull @Min(1) @Max(5) Integer rating,
		@Size(max = 2000) String comment) {
}
