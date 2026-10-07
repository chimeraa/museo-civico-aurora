package museo.civico.aurora.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock UTC iniettabile: l'applicazione usa {@link Clock#systemUTC()}, i test
 * forniscono un clock fisso per rendere deterministici i boundary.
 */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
