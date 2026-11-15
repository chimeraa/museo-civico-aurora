package museo.civico.aurora;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

// Controlla che il database di test parta e sia PostgreSQL 17.
class EmbeddedPostgresSmokeTest {

	// Avvia PostgreSQL e controlla la versione.
	@Test
	void startsPostgres17AndReportsVersion() throws Exception {
		try (EmbeddedPostgres pg = EmbeddedPostgres.start();
				Connection connection = pg.getPostgresDatabase().getConnection();
				Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery("SELECT version()")) {

			assertTrue(rs.next(), "Expected a row from SELECT version()");
			String version = rs.getString(1);
			assertNotNull(version, "PostgreSQL version string must not be null");
			assertTrue(version.startsWith("PostgreSQL 17"),
					"Expected PostgreSQL 17 but was: " + version);
		}
	}

}
