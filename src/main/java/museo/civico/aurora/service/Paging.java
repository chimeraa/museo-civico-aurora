package museo.civico.aurora.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import museo.civico.aurora.api.ApiException;
import museo.civico.aurora.api.ErrorCode;
import museo.civico.aurora.api.FieldError;

// Legge pagina, size e date della richiesta. La pagina parte da 0, size va da 1 a
// 50 e le date sono ISO con offset, con from mai dopo to.
public final class Paging {

	public static final int MAX_SIZE = 50;

	private Paging() {
	}

	public static Pageable validate(int page, int size, Sort sort) {
		if (page < 0) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PAGE,
					"Il parametro 'page' non puo' essere negativo.");
		}
		if (size < 1) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PAGE_SIZE,
					"Il parametro 'size' deve essere almeno 1.");
		}
		// Limite massimo: evita di caricare troppi dati in una volta.
		if (size > MAX_SIZE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PAGE_SIZE,
					"Il parametro 'size' non puo' superare " + MAX_SIZE + ".");
		}
		return PageRequest.of(page, size, sort);
	}

	public static OffsetDateTime parseOptional(String value, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			// Accetta solo date con il fuso orario, per esempio 2026-06-01T10:00:00Z.
			return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
		}
		catch (DateTimeParseException ex) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_TIME_RANGE,
					"Il parametro '" + field + "' non e' una data ISO 8601 con offset.",
					List.of(new FieldError(field, "Usa una data ISO 8601 con offset o Z.")));
		}
	}

	public static void validateRange(OffsetDateTime from, OffsetDateTime to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_TIME_RANGE,
					"L'intervallo di date non e' valido.");
		}
	}
}
