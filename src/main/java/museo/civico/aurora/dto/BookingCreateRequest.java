package museo.civico.aurora.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Prenotazione singola: il client invia solo l'evento. */
public record BookingCreateRequest(@NotNull @Min(1) Long eventId) {
}
