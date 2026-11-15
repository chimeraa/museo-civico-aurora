package museo.civico.aurora.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// Formato della risposta quando c'è un errore.
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorEnvelope(String code, String message, List<FieldError> fieldErrors) {

	// Versione senza errori sui campi.
	public ErrorEnvelope(String code, String message) {
		this(code, message, List.of());
	}
}
