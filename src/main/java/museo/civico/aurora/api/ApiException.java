package museo.civico.aurora.api;

import java.util.List;

import org.springframework.http.HttpStatus;

// Errore nostro, con un codice e uno stato HTTP. Lo prende GlobalExceptionHandler.
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final List<FieldError> fieldErrors;

	// Versione semplice, senza errori sui singoli campi.
	public ApiException(HttpStatus status, String code, String message) {
		this(status, code, message, List.of());
	}

	// Versione con anche gli errori dei campi.
	public ApiException(HttpStatus status, String code, String message, List<FieldError> fieldErrors) {
		super(message);
		this.status = status;
		this.code = code;
		// Lista immutabile.
		this.fieldErrors = fieldErrors == null ? List.of() : List.copyOf(fieldErrors);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public List<FieldError> getFieldErrors() {
		return fieldErrors;
	}
}
