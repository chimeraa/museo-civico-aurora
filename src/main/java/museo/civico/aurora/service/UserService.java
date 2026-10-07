package museo.civico.aurora.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import museo.civico.aurora.api.ApiException;
import museo.civico.aurora.api.ErrorCode;
import museo.civico.aurora.api.FieldError;
import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.Role;
import museo.civico.aurora.dto.RegisterRequest;
import museo.civico.aurora.dto.UserDto;
import museo.civico.aurora.repository.AppUserRepository;

@Service
public class UserService {

	private final AppUserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	public UserService(AppUserRepository users, PasswordEncoder passwordEncoder, Clock clock) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
	}

	@Transactional
	public UserDto register(RegisterRequest request) {
		String email = request.email().trim().toLowerCase(Locale.ROOT);
		String password = request.password();

		if (!PasswordPolicy.isValid(password)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
					"Alcuni campi non sono validi.",
					List.of(new FieldError("password",
							"La password deve avere almeno 8 caratteri e non superare 72 byte.")));
		}

		if (users.existsByEmailIgnoreCase(email)) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.EMAIL_EXISTS,
					"Esiste gia' un account con questa email.");
		}

		AppUser user = new AppUser(email, request.displayName().trim(), passwordEncoder.encode(password), Role.VISITOR,
				OffsetDateTime.now(clock));
		try {
			user = users.saveAndFlush(user);
		}
		catch (DataIntegrityViolationException ex) {
			// Violazione concorrente dell'unicita' case-insensitive: mai 500.
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.EMAIL_EXISTS,
					"Esiste gia' un account con questa email.");
		}
		return toDto(user);
	}

	public static UserDto toDto(AppUser user) {
		return new UserDto(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
	}
}
