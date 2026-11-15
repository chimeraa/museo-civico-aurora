package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

// Riga breve di un evento nell'elenco pubblico.
public record EventSummaryDto(Long id, String title, String location, OffsetDateTime startsAt, OffsetDateTime endsAt,
		int seatsAvailable) {
}
