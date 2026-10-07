package museo.civico.aurora.dto;

import museo.civico.aurora.domain.Role;

/** DTO pubblico utente: mai password o hash. */
public record UserDto(Long id, String email, String displayName, Role role) {
}
