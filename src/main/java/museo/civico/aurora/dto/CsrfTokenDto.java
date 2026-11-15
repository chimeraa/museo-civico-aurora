package museo.civico.aurora.dto;

// Token CSRF con i nomi di header e cookie, da passare al frontend.
public record CsrfTokenDto(String token, String headerName, String parameterName, String cookieName) {
}
