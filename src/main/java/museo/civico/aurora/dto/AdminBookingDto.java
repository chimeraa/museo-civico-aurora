package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

import museo.civico.aurora.domain.BookingStatus;

/** Prenotazione in vista amministrativa: aggiunge solo il nome visualizzato. */
public record AdminBookingDto(Long id, Long eventId, String eventTitle, OffsetDateTime startsAt, BookingStatus status,
		OffsetDateTime createdAt, OffsetDateTime cancelledAt, String visitorDisplayName) {
}
