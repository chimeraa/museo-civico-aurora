package museo.civico.aurora.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import museo.civico.aurora.service.PasswordPolicy;

// Cifra le password con BCrypt. BCrypt accetta solo 72 byte.
public class BoundedBcryptPasswordEncoder implements PasswordEncoder {

	private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder();

	@Override
	public String encode(CharSequence rawPassword) {
		return delegate.encode(rawPassword);
	}

	// Se la password è troppo lunga restituisce false, senza dare errore.
	@Override
	public boolean matches(CharSequence rawPassword, String encodedPassword) {
		if (rawPassword == null || PasswordPolicy.exceedsBcryptLimit(rawPassword.toString())) {
			return false;
		}
		// Solo se la password rientra nel limite si fa il confronto vero.
		return delegate.matches(rawPassword, encodedPassword);
	}

	@Override
	public boolean upgradeEncoding(String encodedPassword) {
		return delegate.upgradeEncoding(encodedPassword);
	}
}
