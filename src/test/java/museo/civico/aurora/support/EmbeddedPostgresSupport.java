package museo.civico.aurora.support;

import java.io.IOException;

import org.springframework.test.context.DynamicPropertyRegistry;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Avvia una sola istanza condivisa di PostgreSQL 17 embedded (Zonky) e la
 * registra come DataSource del contesto Spring. Nessuna dipendenza da Docker o
 * dal PostgreSQL locale.
 */
public final class EmbeddedPostgresSupport {

	private static EmbeddedPostgres instance;

	private EmbeddedPostgresSupport() {
	}

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
					// chiusura best-effort allo spegnimento della JVM
				}
			}));
		}
		return instance;
	}

	public static void registerProperties(DynamicPropertyRegistry registry) {
		EmbeddedPostgres pg = get();
		registry.add("spring.datasource.url", () -> pg.getJdbcUrl("postgres", "postgres"));
		registry.add("spring.datasource.username", () -> "postgres");
		registry.add("spring.datasource.password", () -> "postgres");
	}
}
