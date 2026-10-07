package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

/** Riga sintetica del catalogo pubblico. */
public record EventSummaryDto(Long id, String title, String location, OffsetDateTime startsAt, OffsetDateTime endsAt,
		int seatsAvailable) {
}
