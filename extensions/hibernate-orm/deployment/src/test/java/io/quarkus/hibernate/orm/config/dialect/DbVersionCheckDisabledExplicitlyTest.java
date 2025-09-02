package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that DB version checks can be disabled explicitly.
 * <p>
 * This was originally introduced to work around problems with version checks,
 * such as https://github.com/quarkusio/quarkus/issues/43703 /
 * https://github.com/quarkusio/quarkus/issues/42255
 */
public class DbVersionCheckDisabledExplicitlyTest {

    private static final String ACTUAL_H2_VERSION = DialectVersions.Defaults.H2;
    // We will set the DB version to something higher than the actual version: this is invalid.
    private static final String CONFIGURED_DB_VERSION = "999.999.0";
    static {
        assertThat(ACTUAL_H2_VERSION)
                .as("Test setup - we need the required version to be different from the actual one")
                .doesNotStartWith(CONFIGURED_DB_VERSION);
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SmokeTestUtils.class)
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.db-version", "999.999")
            // We disable the version check explicitly, so Quarkus should boot just fine
            .overrideConfigKey("quarkus.hibernate-orm.database.version-check.enabled", "false");

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    @Test
    public void dialectVersion() {
        var dialectVersion = sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect().getVersion();
        assertThat(DialectVersions.toString(dialectVersion)).isEqualTo(CONFIGURED_DB_VERSION);
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(session,
                MyEntity.class, MyEntity::new,
                MyEntity::getId,
                MyEntity::setName, MyEntity::getName);
    }
}
