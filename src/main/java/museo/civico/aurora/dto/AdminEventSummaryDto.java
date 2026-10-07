package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

import museo.civico.aurora.domain.EventStatus;

/** Riga sintetica amministrativa, include lo stato (anche bozza/annullato). */
public record AdminEventSummaryDto(Long id, String title, String location, OffsetDateTime startsAt,
		OffsetDateTime endsAt, int seatsAvailable, EventStatus status) {
}
