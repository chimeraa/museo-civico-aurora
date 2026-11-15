package museo.civico.aurora.support;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// Orologio fisso: nei test le date restano sempre le stesse.
@TestConfiguration
public class TestClockConfig {

	public static final OffsetDateTime FIXED_NOW = OffsetDateTime.parse("2026-06-01T10:00:00Z");

	// Orologio fermo alle 10:00 UTC del 1 giugno 2026.
	@Bean
	@Primary
	public Clock testClock() {
		return Clock.fixed(Instant.parse("2026-06-01T10:00:00Z"), ZoneOffset.UTC);
	}
}
