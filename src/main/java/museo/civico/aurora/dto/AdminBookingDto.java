package museo.civico.aurora.dto;

import java.time.OffsetDateTime;

import museo.civico.aurora.domain.BookingStatus;

// Prenotazione vista dall'admin. Mostra il nome del visitatore, mai l'email.
public record AdminBookingDto(Long id, Long eventId, String eventTitle, OffsetDateTime startsAt, BookingStatus status,
		OffsetDateTime createdAt, OffsetDateTime cancelledAt, String visitorDisplayName) {
}
