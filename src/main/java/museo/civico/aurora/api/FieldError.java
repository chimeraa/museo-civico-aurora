package museo.civico.aurora.api;

// Errore di un singolo campo, per esempio un'email sbagliata.
public record FieldError(String field, String message) {
}
