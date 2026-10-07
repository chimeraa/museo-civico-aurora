package museo.civico.aurora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import museo.civico.aurora.security.BoundedBcryptPasswordEncoder;
import museo.civico.aurora.security.JsonLoginFailureHandler;
import museo.civico.aurora.security.JsonLoginSuccessHandler;
import museo.civico.aurora.security.RestAccessDeniedHandler;
import museo.civico.aurora.security.RestAuthenticationEntryPoint;

/**
 * Sicurezza HTTP: catena API ordinata prima della catena statica del frontend.
 * Nessuna configurazione CORS.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BoundedBcryptPasswordEncoder();
	}

	/**
	 * Repository CSRF per SPA: cookie leggibile dal frontend, header e nomi
	 * allineati al contratto frozen ({@code X-XSRF-TOKEN} / {@code XSRF-TOKEN}).
	 * Il cookie e' sempre {@code SameSite=Lax}; l'attributo {@code Secure} segue
	 * {@code app.csrf.cookie.secure} (false in locale, true nel profilo HTTPS).
	 */
	@Bean
	public CsrfTokenRepository csrfTokenRepository(
			@Value("${app.csrf.cookie.secure:false}") boolean csrfCookieSecure) {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setHeaderName("X-XSRF-TOKEN");
		repository.setParameterName("_csrf");
		repository.setCookieName("XSRF-TOKEN");
		repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax").secure(csrfCookieSecure));
		return repository;
	}

	@Bean
	@Order(1)
	public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http,
			RestAuthenticationEntryPoint authenticationEntryPoint,
			RestAccessDeniedHandler accessDeniedHandler,
			JsonLoginSuccessHandler loginSuccessHandler,
			JsonLoginFailureHandler loginFailureHandler,
			CsrfTokenRepository csrfTokenRepository) throws Exception {
		http
				.securityMatcher("/api/**")
				.authorizeHttpRequests(auth -> auth
						.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.formLogin(form -> form
						.loginProcessingUrl("/api/auth/login")
						.successHandler(loginSuccessHandler)
						.failureHandler(loginFailureHandler))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler(
								(request, response, authentication) -> response
										.setStatus(HttpServletResponse.SC_NO_CONTENT))
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("JSESSIONID"))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain frontendSecurityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(auth -> auth
						.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
						.requestMatchers("/", "/index.html", "/css/**", "/js/**", "/assets/**", "/favicon.ico",
								"/error")
						.permitAll()
						.anyRequest().permitAll())
				.csrf(csrf -> csrf.disable());
		return http.build();
	}
}
