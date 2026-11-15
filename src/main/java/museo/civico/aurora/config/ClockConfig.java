package museo.civico.aurora.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Orologio unico per tutta l'app, in UTC.
@Configuration
public class ClockConfig {

	// Restituisce l'orologio di sistema in UTC.
	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
