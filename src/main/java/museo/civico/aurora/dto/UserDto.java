package museo.civico.aurora.dto;

import museo.civico.aurora.domain.Role;

// Dati pubblici dell'utente. Non contiene mai la password.
public record UserDto(Long id, String email, String displayName, Role role) {
}
