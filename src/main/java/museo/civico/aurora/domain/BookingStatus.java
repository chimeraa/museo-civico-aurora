package museo.civico.aurora.domain;

// Stato della prenotazione: confermata oppure annullata.
public enum BookingStatus {
	// Il posto è occupato.
	CONFIRMED,
	// Il posto è libero di nuovo. La riga resta, come storico.
	CANCELLED
}
