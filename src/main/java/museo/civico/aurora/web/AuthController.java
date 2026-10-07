package museo.civico.aurora.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DeferredCsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import museo.civico.aurora.dto.CsrfTokenDto;
import museo.civico.aurora.dto.RegisterRequest;
import museo.civico.aurora.dto.UserDto;
import museo.civico.aurora.security.AppUserPrincipal;
import museo.civico.aurora.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";

	private final UserService userService;

	public AuthController(UserService userService) {
		this.userService = userService;
	}

	/**
	 * Materializza il token CSRF (cookie XSRF-TOKEN) e ne restituisce i nomi.
	 * Accessibile prima del login.
	 */
	@GetMapping("/csrf")
	public CsrfTokenDto csrf(CsrfToken csrfToken, HttpServletRequest request) {
		DeferredCsrfToken deferred = (DeferredCsrfToken) request.getAttribute(DeferredCsrfToken.class.getName());
		String token = deferred != null ? deferred.get().getToken() : csrfToken.getToken();
		return new CsrfTokenDto(token, csrfToken.getHeaderName(), csrfToken.getParameterName(), CSRF_COOKIE_NAME);
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public UserDto register(@Valid @RequestBody RegisterRequest request) {
		return userService.register(request);
	}

	@GetMapping("/me")
	public UserDto me(@AuthenticationPrincipal AppUserPrincipal principal) {
		return new UserDto(principal.getId(), principal.getUsername(), principal.getDisplayName(), principal.getRole());
	}
}
