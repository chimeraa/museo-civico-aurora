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
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

// Prende gli errori dei controller e li trasforma in risposte JSON.
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	// Errori creati da noi: usa il codice e lo stato che abbiamo scritto.
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorEnvelope> handleApi(ApiException ex) {
		return ResponseEntity.status(ex.getStatus())
				.body(new ErrorEnvelope(ex.getCode(), ex.getMessage(), ex.getFieldErrors()));
	}

	// Dati non validi: risponde 400 e dice quali campi sono sbagliati.
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorEnvelope> handleValidation(MethodArgumentNotValidException ex) {
		List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest()
				.body(new ErrorEnvelope(ErrorCode.VALIDATION_ERROR, "Alcuni campi non sono validi.", errors));
	}

	// Il JSON non si legge: risponde 400.
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorEnvelope> handleUnreadable(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest()
				.body(new ErrorEnvelope(ErrorCode.VALIDATION_ERROR, "Il corpo della richiesta non e' valido."));
	}

	// Parametro del tipo sbagliato, per esempio testo al posto di un numero: 400.
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorEnvelope> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return ResponseEntity.badRequest()
				.body(new ErrorEnvelope(ErrorCode.VALIDATION_ERROR, "Parametro non valido."));
	}

	// Formato non accettato, per esempio testo invece di JSON: 415.
	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorEnvelope> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
				.body(new ErrorEnvelope(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "Formato della richiesta non supportato."));
	}

	// Errore del database, per esempio un dato doppio. Risponde 409 e non 500.
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

	// Risorsa che non esiste: risponde 404.
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorEnvelope> handleNotFound(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ErrorEnvelope(ErrorCode.NOT_FOUND, "Risorsa non trovata."));
	}

	// Metodo HTTP sbagliato per questa strada: risponde 405.
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorEnvelope> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
				.body(new ErrorEnvelope(ErrorCode.METHOD_NOT_ALLOWED, "Metodo HTTP non supportato per questa risorsa."));
	}

	// Il client chiede un formato che non diamo: risponde 406.
	@ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
	public ResponseEntity<ErrorEnvelope> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
		return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
				.body(new ErrorEnvelope(ErrorCode.NOT_ACCEPTABLE,
						"Nessuna rappresentazione disponibile per il formato richiesto."));
	}

	// Errore imprevisto: scrive il dettaglio nel log e risponde 500.
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorEnvelope> handleGeneric(Exception ex) {
		log.error("Errore non gestito", ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ErrorEnvelope(ErrorCode.INTERNAL_ERROR, "Errore interno del server."));
	}

	// Scorciatoia per le risposte 409.
	private static ResponseEntity<ErrorEnvelope> conflict(String code, String message) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorEnvelope(code, message));
	}
}
