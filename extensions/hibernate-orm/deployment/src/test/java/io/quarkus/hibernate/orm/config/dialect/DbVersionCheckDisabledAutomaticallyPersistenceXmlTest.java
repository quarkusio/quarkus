package io.quarkus.hibernate.orm.config.dialect;

import static io.quarkus.hibernate.orm.ResourceUtil.loadResourceAndReplacePlaceholders;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that the workaround for https://github.com/quarkusio/quarkus/issues/43703 /
 * https://github.com/quarkusio/quarkus/issues/42255
 * is effective.
 */
// TODO remove this test when change the default to "always enabled" when we solve version detection problems
//   See https://github.com/quarkusio/quarkus/issues/43703
//   See https://github.com/quarkusio/quarkus/issues/42255
public class DbVersionCheckDisabledAutomaticallyPersistenceXmlTest {

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
                    .addClass(MyEntity.class)
                    .addAsManifestResource(new StringAsset(loadResourceAndReplacePlaceholders(
                            "META-INF/some-persistence-with-h2-version-placeholder-and-explicit-dialect.xml",
                            Map.of("H2_VERSION", "999.999"))),
                            "persistence.xml"))
            .withConfigurationResource("application-datasource-only.properties");

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
