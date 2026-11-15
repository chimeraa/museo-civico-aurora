package museo.civico.aurora.support;

import java.io.IOException;

import org.springframework.test.context.DynamicPropertyRegistry;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

// Avvia un solo PostgreSQL di prova per tutti i test.
public final class EmbeddedPostgresSupport {

	private static EmbeddedPostgres instance;

	private EmbeddedPostgresSupport() {
	}

	// Ritorna il database di prova. Lo avvia solo alla prima chiamata.
	public static synchronized EmbeddedPostgres get() {
		if (instance == null) {
			try {
				instance = EmbeddedPostgres.start();
			}
			catch (IOException ex) {
				throw new IllegalStateException("Impossibile avviare PostgreSQL embedded", ex);
			}
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					instance.close();
				}
				catch (Exception ignored) {
					// Se la chiusura va male non c'è altro da fare.
				}
			}));
		}
		return instance;
	}

	// Passa a Spring indirizzo e credenziali del database di prova.
	public static void registerProperties(DynamicPropertyRegistry registry) {
		EmbeddedPostgres pg = get();
		registry.add("spring.datasource.url", () -> pg.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "postgres");
	}
}
