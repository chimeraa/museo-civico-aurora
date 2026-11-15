package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

import museo.civico.aurora.domain.EventStatus;

// Tutti i dati di un evento.
public record EventDetailDto(Long id, String title, String location, OffsetDateTime startsAt, OffsetDateTime endsAt,
		int seatsAvailable, String description, int capacity, EventStatus status) {
}
