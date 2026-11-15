package museo.civico.aurora.service;

import java.nio.charset.StandardCharsets;

// Regole per le password: almeno 8 caratteri e al massimo 72 byte.
public final class PasswordPolicy {

	public static final int MIN_LENGTH = 8;
	public static final int MAX_BCRYPT_BYTES = 72;

	private PasswordPolicy() {
	}

	public static boolean isTooShort(String password) {
		// Conta i caratteri, non i byte.
		return password == null || password.length() < MIN_LENGTH;
	}

	// Dice se la password supera i 72 byte di BCrypt.
	public static boolean exceedsBcryptLimit(String password) {
		return password != null && password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES;
	}

	public static boolean isValid(String password) {
		return !isTooShort(password) && !exceedsBcryptLimit(password);
	}
}
