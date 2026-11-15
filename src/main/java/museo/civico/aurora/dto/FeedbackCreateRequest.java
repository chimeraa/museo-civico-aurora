package museo.civico.aurora.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Dati del giudizio: voto da 1 a 5 e commento facoltativo.
public record FeedbackCreateRequest(
		@NotNull @Min(1) @Max(5) Integer rating,
		@Size(max = 2000) String comment) {
}
