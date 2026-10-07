package museo.civico.aurora.dto;

/** Token CSRF materializzato per il frontend. */
public record CsrfTokenDto(String token, String headerName, String parameterName, String cookieName) {
}
