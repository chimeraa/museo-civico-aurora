package museo.civico.aurora.config;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.Role;
import museo.civico.aurora.repository.AppUserRepository;
import museo.civico.aurora.service.PasswordPolicy;

/**
 * Crea l'admin di sviluppo solo quando il profilo {@code dev} e' attivo e
 * {@code APP_ADMIN_EMAIL} e {@code APP_ADMIN_PASSWORD} sono entrambi
 * valorizzati. Nessun default e nessun log di password/hash. I test non
 * dipendono da queste variabili.
 */
@Component
@Profile("dev")
public class AdminSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(AdminSeedRunner.class);

	private final AppUserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final String adminEmail;
	private final String adminPassword;

	public AdminSeedRunner(AppUserRepository users, PasswordEncoder passwordEncoder, Clock clock,
			@Value("${app.admin.email:}") String adminEmail,
			@Value("${app.admin.password:}") String adminPassword) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.clock = clock;
		this.adminEmail = adminEmail;
		this.adminPassword = adminPassword;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
			return;
		}
		if (!PasswordPolicy.isValid(adminPassword)) {
			log.warn("APP_ADMIN_PASSWORD non rispetta i requisiti; nessun admin creato.");
			return;
		}
		String email = adminEmail.trim().toLowerCase(Locale.ROOT);
		if (users.existsByEmailIgnoreCase(email)) {
			return;
		}
		users.save(new AppUser(email, "Amministratore", passwordEncoder.encode(adminPassword), Role.ADMIN,
				OffsetDateTime.now(clock)));
		log.info("Account amministratore di sviluppo creato.");
	}
}
