package museo.civico.aurora.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// Dati per prenotare: basta l'id dell'evento.
public record BookingCreateRequest(@NotNull @Min(1) Long eventId) {
}
