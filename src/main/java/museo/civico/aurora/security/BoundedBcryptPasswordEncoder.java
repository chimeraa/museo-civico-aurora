package museo.civico.aurora.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import museo.civico.aurora.service.PasswordPolicy;

/**
 * {@link PasswordEncoder} limitato al massimo BCrypt (72 byte UTF-8). L'
 * {@code encode} delega al BCrypt standard (la registrazione valida gia' la
 * lunghezza), mentre {@code matches} restituisce {@code false} per input oltre
 * il limite invece di propagare un errore: un login con password troppo lunga
 * produce {@code 401 INVALID_CREDENTIALS}, mai {@code 500}.
 */
public class BoundedBcryptPasswordEncoder implements PasswordEncoder {

	private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

	@Override
	public String encode(CharSequence rawPassword) {
		return delegate.encode(rawPassword);
	}

	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if (rawPassword == null || PasswordPolicy.exceedsBcryptLimit(rawPassword.toString())) {
			return false;
		}
		return delegate.matches(rawPassword, encodedPassword);
	}

	@Override
	public boolean upgradeEncoding(String encodedPassword) {
		return delegate.upgradeEncoding(encodedPassword);
	}
}
