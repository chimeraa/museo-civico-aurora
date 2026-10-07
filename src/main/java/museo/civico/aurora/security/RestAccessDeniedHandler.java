package museo.civico.aurora.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import museo.civico.aurora.api.ErrorCode;
import museo.civico.aurora.api.ErrorEnvelope;
import tools.jackson.databind.json.JsonMapper;

/**
 * Access denied handler JSON condiviso: riconosce le eccezioni CSRF e le
 * distingue dal diniego di ruolo.
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

	private final JsonMapper jsonMapper;

	public RestAccessDeniedHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		String code = (accessDeniedException instanceof CsrfException) ? ErrorCode.CSRF_INVALID
				: ErrorCode.FORBIDDEN;
		String message = (accessDeniedException instanceof CsrfException)
				? "Token CSRF assente o non valido."
				: "Non hai i permessi per questa operazione.";

		response.setStatus(HttpStatus.FORBIDDEN.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getOutputStream(), new ErrorEnvelope(code, message));
	}
}
