package museo.civico.aurora.domain;

// Stato dell'evento: bozza, pubblicato o annullato. Annullato è l'ultimo stato.
public enum EventStatus {
	// In preparazione: visibile solo all'admin.
	DRAFT,
	// Visibile al pubblico e prenotabile.
	PUBLISHED,
	// Annullato: non si torna indietro e le prenotazioni sono annullate.
	CANCELLED
}
