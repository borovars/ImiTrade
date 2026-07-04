package ImiTrade.testsupport;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for full-stack integration tests that need a real PostgreSQL instance.
 *
 * <p>Spins up a PostgreSQL 17 container via Testcontainers and wires it into the
 * Spring context with {@link ServiceConnection}, so the production datasource
 * configuration (PostgreSQL driver + Flyway {@code V1..V3} migrations) runs against
 * it unchanged. Deliberately does NOT activate the {@code test} profile, which
 * would otherwise switch the datasource to in-memory H2 and disable Flyway.
 */
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class PostgresTestBase {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("imitrade")
                    .withUsername("test")
                    .withPassword("test");
}
