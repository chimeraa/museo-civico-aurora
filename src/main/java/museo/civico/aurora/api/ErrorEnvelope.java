package museo.civico.aurora.api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Envelope di errore unico per controller e componenti Security filter-level.
 * {@code fieldErrors} e' omesso quando vuoto.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorEnvelope(String code, String message, List<FieldError> fieldErrors) {

	public ErrorEnvelope(String code, String message) {
		this(code, message, List.of());
	}
}
