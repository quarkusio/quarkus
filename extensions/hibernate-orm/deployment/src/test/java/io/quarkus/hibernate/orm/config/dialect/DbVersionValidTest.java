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

public class DbVersionValidTest {

    private static final String ACTUAL_H2_VERSION = DialectVersions.Defaults.H2;
    private static final String CONFIGURED_DB_VERSION;
    static {
        // We will set the DB version to something lower than the actual version: this is valid.
        CONFIGURED_DB_VERSION = ACTUAL_H2_VERSION.replaceAll("\\.[\\d]+\\.[\\d]+$", ".0.0");
        assertThat(ACTUAL_H2_VERSION)
                .as("Test setup - we need the required version to be different from the actual one")
                .isNotEqualTo(CONFIGURED_DB_VERSION);
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(SmokeTestUtils.class).addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.db-version", CONFIGURED_DB_VERSION);

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    @Test
    public void dialectVersion() {
        var dialectVersion = sessionFactory.unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect()
                .getVersion();
        assertThat(DialectVersions.toString(dialectVersion)).isEqualTo(CONFIGURED_DB_VERSION);
    }

    @Test
    @Transactional
    public void smokeTest() {
        SmokeTestUtils.testSimplePersistRetrieveUpdateDelete(session, MyEntity.class, MyEntity::new, MyEntity::getId,
                MyEntity::setName, MyEntity::getName);
    }
}
