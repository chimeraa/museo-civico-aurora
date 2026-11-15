package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

import museo.civico.aurora.domain.EventStatus;

// Riga di un evento nell'elenco dell'admin. Qui compaiono anche bozze e annullati.
public record AdminEventSummaryDto(Long id, String title, String location, OffsetDateTime startsAt,
		OffsetDateTime endsAt, int seatsAvailable, EventStatus status) {
}
