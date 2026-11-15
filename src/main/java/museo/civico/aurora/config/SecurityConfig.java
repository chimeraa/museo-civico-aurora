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

// Regole di sicurezza: chi può vedere cosa, login, logout e token CSRF.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	// Il modo in cui le password vengono cifrate.
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BoundedBcryptPasswordEncoder();
	}

	// Il token CSRF va in un cookie che il frontend può leggere.
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

	// Regole per le API (/api/...). Risponde sempre in JSON.
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
						// Il catalogo eventi è pubblico: lo vedono anche gli ospiti senza login.
						.requestMatchers(HttpMethod.GET, "/api/events/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
						// Solo l'admin entra nelle pagine /api/admin.
						.requestMatchers("/api/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository))
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
				.formLogin(form -> form
						.loginProcessingUrl("/api/auth/login")
						.successHandler(loginSuccessHandler)
						.failureHandler(loginFailureHandler))
				// Il logout cancella la sessione e il cookie JSESSIONID.
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

	// Pagine del sito (html, css, js): accessibili a tutti.
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
				// Le pagine statiche non cambiano dati, quindi il CSRF non serve.
				.csrf(csrf -> csrf.disable());
		return http.build();
	}
}
