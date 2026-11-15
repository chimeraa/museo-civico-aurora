package museo.civico.aurora.support;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import jakarta.servlet.http.Cookie;
import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.CulturalEvent;
import museo.civico.aurora.domain.EventStatus;
import museo.civico.aurora.domain.Role;
import museo.civico.aurora.repository.AppUserRepository;
import museo.civico.aurora.repository.BookingRepository;
import museo.civico.aurora.repository.CulturalEventRepository;
import museo.civico.aurora.repository.FeedbackRepository;
import museo.civico.aurora.security.AppUserPrincipal;

// Base per i test di integrazione: avvia l'app con un database di prova e un orologio fisso.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class AbstractIntegrationTest {

	public static final OffsetDateTime NOW = TestClockConfig.FIXED_NOW;
	public static final OffsetDateTime FUTURE_START = NOW.plusDays(9);
	public static final OffsetDateTime FUTURE_END = NOW.plusDays(9).plusHours(2);
	public static final OffsetDateTime PAST_START = NOW.minusDays(3);
	public static final OffsetDateTime PAST_END = NOW.minusDays(3).plusHours(2);

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected AppUserRepository userRepository;

	@Autowired
	protected CulturalEventRepository eventRepository;

	@Autowired
	protected BookingRepository bookingRepository;

	@Autowired
	protected FeedbackRepository feedbackRepository;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Autowired
	protected JdbcTemplate jdbcTemplate;

	@Autowired
	protected Clock clock;

	// Dice a Spring dove trovare il database di prova.
	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		EmbeddedPostgresSupport.registerProperties(registry);
	}

	// Svuota le tabelle prima di ogni test.
	@BeforeEach
	void cleanDatabase() {
		jdbcTemplate.execute(
				"TRUNCATE TABLE feedback, booking, cultural_event, app_user RESTART IDENTITY CASCADE");
	}

	// Crea un visitatore di prova.
	protected AppUser createVisitor(String email) {
		return createUser(email, "Visitatore", Role.VISITOR, "password123");
	}

	// Crea un admin di prova.
	protected AppUser createAdmin(String email) {
		return createUser(email, "Amministratore", Role.ADMIN, "password123");
	}

	// Crea un utente, con la password salvata cifrata.
	protected AppUser createUser(String email, String displayName, Role role, String rawPassword) {
		AppUser user = new AppUser(email.trim().toLowerCase(Locale.ROOT), displayName,
				passwordEncoder.encode(rawPassword), role, NOW);
		return userRepository.saveAndFlush(user);
	}

	// Dati di sessione per un utente di prova.
	protected AppUserPrincipal principalOf(AppUser user) {
		return AppUserPrincipal.from(user);
	}

	// Salva un evento di prova nel database.
	protected CulturalEvent persistEvent(String title, EventStatus status, int capacity, OffsetDateTime startsAt,
			OffsetDateTime endsAt) {
		CulturalEvent event = new CulturalEvent(title, "Descrizione di test", "Sala Aurora", startsAt, endsAt,
				capacity, status, NOW, NOW);
		return eventRepository.saveAndFlush(event);
	}

	// Salva un evento pubblicato che inizia nel futuro.
	protected CulturalEvent persistPublishedFutureEvent(String title, int capacity) {
		return persistEvent(title, EventStatus.PUBLISHED, capacity, FUTURE_START, FUTURE_END);
	}

	// Prende un token CSRF vero e lo mette nella richiesta.
	protected RequestPostProcessor csrfToken() throws Exception {
		MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.get("/api/auth/csrf"))
				.andReturn();
		Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
		if (cookie == null) {
			throw new IllegalStateException("Cookie CSRF XSRF-TOKEN non materializzato");
		}
		String token = cookie.getValue();
		return request -> {
			request.setCookies(cookie);
			request.addHeader("X-XSRF-TOKEN", token);
			return request;
		};
	}
}
