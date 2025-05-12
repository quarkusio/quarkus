package io.quarkus.hibernate.reactive.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.DialectUtils;
import io.quarkus.hibernate.reactive.MyEntity;
import io.quarkus.hibernate.reactive.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

public class DbVersionValidTest {

    // We will set the DB version to something lower than the actual version: this is valid.
    private static final String CONFIGURED_DB_VERSION = "16.0";
    private static final String CONFIGURED_DB_VERSION_REPORTED;
    static {
        // For some reason Hibernate ORM infers a micro version of 0; no big deal.
        CONFIGURED_DB_VERSION_REPORTED = CONFIGURED_DB_VERSION + ".0";
    }
    static {
        assertThat(DialectUtils.getDefaultVersion(PostgreSQLDialect.class))
                .as("Test setup - we need the required version to be different from the default")
                .doesNotStartWith(CONFIGURED_DB_VERSION + ".");
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(DialectUtils.class)
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.db-version", CONFIGURED_DB_VERSION);

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    public void dialectVersion() {
        var dialectVersion = DialectUtils.getConfiguredVersion(sessionFactory);
        assertThat(dialectVersion).isEqualTo(CONFIGURED_DB_VERSION_REPORTED);
    }

    @Test
    @RunOnVertxContext
    public void smokeTest(UniAsserter asserter) {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(asserter, sessionFactory,
                MyEntity.class, MyEntity::new,
                MyEntity::getId,
                MyEntity::setName, MyEntity::getName);
    }
}
