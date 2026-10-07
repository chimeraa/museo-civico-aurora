package museo.civico.aurora.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Registrazione pubblica: crea sempre un VISITOR. */
public record RegisterRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotBlank @Size(min = 8) String password,
		@NotBlank @Size(min = 1, max = 100) String displayName) {
}
