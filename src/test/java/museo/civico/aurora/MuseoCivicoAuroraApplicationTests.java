package museo.civico.aurora;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import museo.civico.aurora.support.AbstractIntegrationTest;

/**
 * T11 (parte): il contesto si avvia con Flyway come fonte DDL e
 * {@code ddl-auto=validate} su PostgreSQL 17. Verifica anche gli invarianti
 * strutturali principali (indice univoco parziale).
 */
class MuseoCivicoAuroraApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoadsWithMigratedSchema() {
		List<String> tables = jdbcTemplate
				.queryForList("select table_name from information_schema.tables where table_schema = 'public'")
				.stream()
				.map(row -> String.valueOf(row.get("table_name")))
				.toList();
		assertThat(tables).contains("app_user", "cultural_event", "booking", "feedback", "flyway_schema_history");

		Integer applied = jdbcTemplate.queryForObject(
				"select count(*) from flyway_schema_history where success = true", Integer.class);
		assertThat(applied).isNotNull().isGreaterThanOrEqualTo(1);

		List<Map<String, Object>> bookingIndexes = jdbcTemplate
				.queryForList("select indexname from pg_indexes where tablename = 'booking'");
		assertThat(bookingIndexes).extracting(row -> String.valueOf(row.get("indexname")))
				.contains("ux_booking_active");
	}
}
