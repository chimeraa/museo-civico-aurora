package museo.civico.aurora.api;

/**
 * Voce di errore di campo dell'error envelope: {@code field} e' il nome JSON del
 * campo.
 */
public record FieldError(String field, String message) {
}
