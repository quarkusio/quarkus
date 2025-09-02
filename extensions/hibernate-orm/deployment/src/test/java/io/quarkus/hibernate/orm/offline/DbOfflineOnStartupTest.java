package io.quarkus.hibernate.orm.offline;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

/**
 * If nothing requires DB access on startup, we expect startup to proceed even if the database is not reachable.
 * <p>
 * There will likely be warnings being logged, but the application will start.
 */
public class DbOfflineOnStartupTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            // Disable schema management
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy",
                    "none")
            // Pick a DB URL that is offline
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    "jdbc:h2:tcp://localhost:9999/~/sample");

    @Inject
    Session session;

    @Test
    @Transactional
    public void smokeTest() {
        // We expect startup to succeed, but any DB access would obviously fail
        var entity = new MyEntity("someName");
        Assertions.assertThatThrownBy(() -> {
            session.persist(entity);
            session.flush();
        })
                .hasMessageContaining("Connection refused");
    }
}
