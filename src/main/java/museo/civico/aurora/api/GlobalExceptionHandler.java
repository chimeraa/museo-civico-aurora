package museo.civico.aurora.api;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Gestisce le eccezioni dei controller. Le eccezioni sollevate dai filtri
 * Security sono gestite dai loro handler dedicati.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorEnvelope> handleApi(ApiException ex) {
		return ResponseEntity.status(ex.getStatus())
				.body(new ErrorEnvelope(ex.getCode(), ex.getMessage(), ex.getFieldErrors()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
		List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest()
				.body(new ErrorEnvelope(ErrorCode.VALIDATION_ERROR, "Alcuni campi non sono validi.", errors));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorEnvelope> handleUnreadable(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest()
				.body(new ErrorEnvelope(ErrorCode.VALIDATION_ERROR, "Il corpo della richiesta non e' valido."));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorEnvelope> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return ResponseEntity.badRequest()
				.body(new ErrorEnvelope(ErrorCode.VALIDATION_ERROR, "Parametro non valido."));
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorEnvelope> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.body(new ErrorEnvelope(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Formato della richiesta non supportato."));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorEnvelope> handleDataIntegrity(DataIntegrityViolationException ex) {
		Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
		String message = root.getMessage() == null ? "" : root.getMessage().toLowerCase(Locale.ROOT);
		if (message.contains("ux_app_user_email_lower")) {
			return conflict(ErrorCode.EMAIL_EXISTS, "Esiste gia' un account con questa email.");
		}
		if (message.contains("ux_booking_active")) {
			return conflict(ErrorCode.ALREADY_BOOKED, "Esiste gia' una prenotazione attiva per questo evento.");
		}
		if (message.contains("ux_feedback_event_user")) {
			return conflict(ErrorCode.FEEDBACK_EXISTS, "Hai gia' lasciato un giudizio per questo evento.");
		}
		log.warn("Violazione di vincolo dati non mappata: {}", root.getMessage());
		return conflict(ErrorCode.CONFLICT, "Operazione in conflitto con lo stato dei dati.");
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorEnvelope> handleNotFound(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorEnvelope(ErrorCode.NOT_FOUND, "Risorsa non trovata."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorEnvelope> handleGeneric(Exception ex) {
		log.error("Errore non gestito", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorEnvelope(ErrorCode.INTERNAL_ERROR, "Errore interno del server."));
	}

	private static ResponseEntity<ErrorEnvelope> conflict(String code, String message) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorEnvelope(code, message));
	}
}
