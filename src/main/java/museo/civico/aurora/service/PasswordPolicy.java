package museo.civico.aurora.service;

import java.nio.charset.StandardCharsets;

/**
 * Regole password condivise: almeno 8 caratteri e non oltre 72 byte UTF-8
 * (limite BCrypt).
 */
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 8;
	public static final int MAX_BCRYPT_BYTES = 72;

	private PasswordPolicy() {
	}

	public static boolean isTooShort(String password) {
		return password == null || password.length() < MIN_LENGTH;
	}

	public static boolean exceedsBcryptLimit(String password) {
		return password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES;
	}

	public static boolean isValid(String password) {
		return !isTooShort(password) && !exceedsBcryptLimit(password);
	}
}
