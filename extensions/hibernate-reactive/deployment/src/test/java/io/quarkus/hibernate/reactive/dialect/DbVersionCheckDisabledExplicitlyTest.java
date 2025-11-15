package io.quarkus.hibernate.reactive.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.DialectUtils;
import io.quarkus.hibernate.reactive.MyEntity;
import io.quarkus.hibernate.reactive.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Tests that DB version checks can be disabled explicitly.
 * <p>
 * This was originally introduced to work around problems with version checks,
 * such as https://github.com/quarkusio/quarkus/issues/43703 /
 * https://github.com/quarkusio/quarkus/issues/42255
 */
public class DbVersionCheckDisabledExplicitlyTest {

    // We will set the DB version to something higher than the actual version: this is invalid.
    private static final String CONFIGURED_DB_VERSION = "999.999.0";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(DialectUtils.class)
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.db-version", "999.999")
            // We disable the version check explicitly, so Quarkus should boot just fine
            .overrideConfigKey("quarkus.hibernate-orm.database.version-check.enabled", "false");

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    public void dialectVersion() {
        var dialectVersion = DialectUtils.getConfiguredVersion(sessionFactory);
        assertThat(dialectVersion).isEqualTo(CONFIGURED_DB_VERSION);
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
