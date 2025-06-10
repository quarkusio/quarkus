package io.quarkus.hibernate.reactive.offline;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.MyEntity;
import io.quarkus.hibernate.reactive.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

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
            .overrideConfigKey("quarkus.datasource.reactive.url",
                    "vertx-reactive:postgresql://localhost:9999/hibernate_orm_test");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    @RunOnVertxContext
    public void smokeTest(UniAsserter asserter) {
        // We expect startup to succeed, but any DB access would obviously fail
        var entity = new MyEntity("someName");
        asserter.assertFailedWith(
                () -> sessionFactory.withTransaction(session -> session.persist(entity).chain(session::flush)),
                t -> assertThat(t).hasMessageContaining("Connection refused"));
    }
}
