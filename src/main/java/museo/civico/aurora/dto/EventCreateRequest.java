package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import museo.civico.aurora.domain.EventStatus;

/** Richiesta di creazione evento; {@code status} opzionale (default DRAFT). */
public record EventCreateRequest(
		@NotBlank @Size(min = 1, max = 160) String title,
		@Size(max = 5000) String description,
		@NotBlank @Size(min = 1, max = 200) String location,
		@NotNull OffsetDateTime startsAt,
		@NotNull OffsetDateTime endsAt,
		@NotNull @Min(1) Integer capacity,
		EventStatus status) {
}
