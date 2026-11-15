package museo.civico.aurora.domain;

// Stato dell'evento: bozza, pubblicato o annullato. Annullato è l'ultimo stato.
public enum EventStatus {
	DRAFT,
	PUBLISHED,
	CANCELLED
}
