package museo.civico.aurora.domain;

/**
 * Stato del ciclo di vita di un evento culturale. {@link #CANCELLED} e' terminale.
 */
public enum EventStatus {
	DRAFT,
	PUBLISHED,
	CANCELLED
}
