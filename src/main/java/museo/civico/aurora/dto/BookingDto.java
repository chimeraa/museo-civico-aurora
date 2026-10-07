package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

import museo.civico.aurora.domain.BookingStatus;

/** Prenotazione del visitatore. */
public record BookingDto(Long id, Long eventId, String eventTitle, OffsetDateTime startsAt, BookingStatus status,
		OffsetDateTime createdAt, OffsetDateTime cancelledAt) {
}
