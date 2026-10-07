package museo.civico.aurora.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import museo.civico.aurora.api.ErrorCode;
import museo.civico.aurora.api.ErrorEnvelope;
import tools.jackson.databind.json.JsonMapper;

/**
 * Login fallito: risposta JSON 401 con envelope, nessun redirect.
 */
@Component
public class JsonLoginFailureHandler implements AuthenticationFailureHandler {

	private final JsonMapper jsonMapper;

	public JsonLoginFailureHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getOutputStream(),
				new ErrorEnvelope(ErrorCode.INVALID_CREDENTIALS, "Email o password non corretti."));
	}
}
