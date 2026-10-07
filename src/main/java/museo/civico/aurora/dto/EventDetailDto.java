package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

import museo.civico.aurora.domain.EventStatus;

/** Dettaglio evento pubblico/amministrativo. */
public record EventDetailDto(Long id, String title, String location, OffsetDateTime startsAt, OffsetDateTime endsAt,
		int seatsAvailable, String description, int capacity, EventStatus status) {
}
