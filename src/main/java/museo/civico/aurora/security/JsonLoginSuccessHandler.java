package museo.civico.aurora.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import museo.civico.aurora.dto.UserDto;
import tools.jackson.databind.json.JsonMapper;

/**
 * Login riuscito: risposta JSON 200 con lo {@link UserDto}, nessun redirect.
 */
@Component
public class JsonLoginSuccessHandler implements AuthenticationSuccessHandler {

	private final JsonMapper jsonMapper;

	public JsonLoginSuccessHandler(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
		UserDto body = new UserDto(principal.getId(), principal.getUsername(), principal.getDisplayName(),
				principal.getRole());

		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getOutputStream(), body);
	}
}
