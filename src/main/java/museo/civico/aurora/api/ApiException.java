package museo.civico.aurora.api;

import java.util.List;

import org.springframework.http.HttpStatus;

/**
 * Eccezione applicativa mappata sull'error envelope dal
 * {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final List<FieldError> fieldErrors;

	public ApiException(HttpStatus status, String code, String message) {
		this(status, code, message, List.of());
	}

	public ApiException(HttpStatus status, String code, String message, List<FieldError> fieldErrors) {
		super(message);
		this.status = status;
		this.code = code;
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
