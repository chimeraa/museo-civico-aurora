package museo.civico.aurora;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.Role;
import museo.civico.aurora.support.AbstractIntegrationTest;
import museo.civico.aurora.support.JsonTestSupport;

/**
 * T4 (401/redirect), T5 (autorizzazione admin), T6 (CSRF), T7 (registrazione,
 * email, password).
 */
class SecurityIntegrationTest extends AbstractIntegrationTest {

	@Test
	void csrfEndpointExposesTokenAndCookieNames() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
				.andExpect(jsonPath("$.cookieName").value("XSRF-TOKEN"))
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(cookie().exists("XSRF-TOKEN"));
	}

	@Test
	void csrfCookieIsSameSiteLaxAndNotSecureOverLocalHttp() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();

		Cookie csrfCookie = result.getResponse().getCookie("XSRF-TOKEN");
		assertThat(csrfCookie).isNotNull();
		// Servlet 6 mappa il ResponseCookie su un Cookie: SameSite resta un
		// attributo del cookie, non serializzato nel Set-Cookie grezzo da MockMvc.
		assertThat(csrfCookie.getAttribute("SameSite")).isEqualTo("Lax");
		assertThat(csrfCookie.getSecure()).isFalse();

		List<String> setCookies = result.getResponse().getHeaders("Set-Cookie");
		String csrfHeader = setCookies.stream()
				.filter(header -> header.startsWith("XSRF-TOKEN="))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Cookie XSRF-TOKEN non materializzato: " + setCookies));
		assertThat(csrfHeader).doesNotContain("Secure");
	}

	@Test
	void registerCreatesVisitorAndHashesPassword() throws Exception {
		mockMvc.perform(post("/api/auth/register").with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"Mario.Rossi@Example.com","password":"password123","displayName":"Mario","role":"ADMIN"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("mario.rossi@example.com"))
				.andExpect(jsonPath("$.role").value("VISITOR"))
				.andExpect(jsonPath("$.password").doesNotExist());

		AppUser stored = userRepository.findByEmailIgnoreCase("mario.rossi@example.com").orElseThrow();
		assertThat(stored.getRole()).isEqualTo(Role.VISITOR);
		assertThat(stored.getPasswordHash()).startsWith("$2").isNotEqualTo("password123");
		assertThat(passwordEncoder.matches("password123", stored.getPasswordHash())).isTrue();
	}

	@Test
	void registerDuplicateCaseVariantReturnsEmailExists() throws Exception {
		createVisitor("duplicate@example.com");

		mockMvc.perform(post("/api/auth/register").with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"Duplicate@Example.com","password":"password123","displayName":"Dup"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
	}

	@Test
	void registerPasswordOver72BytesReturnsValidationError() throws Exception {
		String longPassword = "a".repeat(73);
		mockMvc.perform(post("/api/auth/register").with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"long@example.com","password":"%s","displayName":"Long"}
								""".formatted(longPassword)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
	}

	@Test
	void loginFormFlowMeAndLogout() throws Exception {
		AppUser visitor = createVisitor("login@example.com");

		MvcResult login = mockMvc.perform(post("/api/auth/login").with(csrfToken())
						.param("username", "login@example.com")
						.param("password", "password123"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.id").value(visitor.getId().intValue()))
				.andExpect(jsonPath("$.email").value("login@example.com"))
				.andReturn();

		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertThat(session).isNotNull();

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("login@example.com"));

		mockMvc.perform(post("/api/auth/logout").session(session).with(csrfToken()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void loginWrongPasswordReturnsJson401() throws Exception {
		createVisitor("wrong@example.com");

		mockMvc.perform(post("/api/auth/login").with(csrfToken())
						.param("username", "wrong@example.com")
						.param("password", "not-the-password"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void loginPasswordOver72BytesReturnsJson401() throws Exception {
		createVisitor("overlong@example.com");
		String overlongPassword = "a".repeat(73);

		mockMvc.perform(post("/api/auth/login").with(csrfToken())
						.param("username", "overlong@example.com")
						.param("password", overlongPassword))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void csrfTokenIsRotatedAndUsableAcrossLoginAndLogout() throws Exception {
		createVisitor("lifecycle@example.com");

		MvcResult initial = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andReturn();
		Cookie initialCookie = initial.getResponse().getCookie("XSRF-TOKEN");
		assertThat(initialCookie).isNotNull();
		String initialToken = JsonTestSupport.extractString(initial.getResponse().getContentAsString(), "token");

		MvcResult login = mockMvc.perform(post("/api/auth/login")
						.cookie(initialCookie)
						.header("X-XSRF-TOKEN", initialToken)
						.param("username", "lifecycle@example.com")
						.param("password", "password123"))
				.andExpect(status().isOk())
				.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertThat(session).isNotNull();

		// GET richiesto dal frontend dopo il login: token/cookie freschi e usabili.
		MvcResult refreshed = mockMvc.perform(get("/api/auth/csrf").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andReturn();
		Cookie refreshedCookie = refreshed.getResponse().getCookie("XSRF-TOKEN");
		assertThat(refreshedCookie).isNotNull();
		String postAuthToken = JsonTestSupport.extractString(refreshed.getResponse().getContentAsString(), "token");
		assertThat(postAuthToken).isNotEqualTo(initialToken);

		// Il token post-auth e' quello usato per la successiva richiesta unsafe.
		mockMvc.perform(post("/api/auth/logout")
						.session(session)
						.cookie(refreshedCookie)
						.header("X-XSRF-TOKEN", postAuthToken))
				.andExpect(status().isNoContent());

		// Dopo il logout il frontend puo' riottenere token/cookie freschi.
		MvcResult afterLogout = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andReturn();
		assertThat(JsonTestSupport.extractString(afterLogout.getResponse().getContentAsString(), "token")).isNotBlank();
	}

	@Test
	void unsafeRequestWithoutCsrfTokenReturnsCsrfInvalid() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"no-csrf@example.com","password":"password123","displayName":"NoCsrf"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
	}

	@Test
	void anonymousBookingWithValidCsrfIsJson401NotRedirect() throws Exception {
		persistPublishedFutureEvent("Evento anonimo", 5);

		mockMvc.perform(post("/api/bookings").with(csrfToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":1}"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void anonymousBookingWithoutCsrfIsCsrfInvalid() throws Exception {
		mockMvc.perform(post("/api/bookings")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"eventId\":1}"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
	}

	@Test
	void adminEndpointsEnforceRole() throws Exception {
		AppUser admin = createAdmin("admin@example.com");
		AppUser visitor = createVisitor("visitor@example.com");

		// Anonimo -> 401 JSON, mai 302.
		mockMvc.perform(get("/api/admin/events"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		// Visitatore autenticato -> 403.
		mockMvc.perform(get("/api/admin/events").with(user(principalOf(visitor))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		// Admin -> 200.
		mockMvc.perform(get("/api/admin/events").with(user(principalOf(admin))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(0));
	}
}
