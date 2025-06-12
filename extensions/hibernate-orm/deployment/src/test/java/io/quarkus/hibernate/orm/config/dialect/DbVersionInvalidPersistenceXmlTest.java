package io.quarkus.hibernate.orm.config.dialect;

import static io.quarkus.hibernate.orm.ResourceUtil.loadResourceAndReplacePlaceholders;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import jakarta.inject.Inject;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.test.QuarkusUnitTest;

public class DbVersionInvalidPersistenceXmlTest {

    private static final String ACTUAL_H2_VERSION = DialectVersions.Defaults.H2;
    // We will set the DB version to something higher than the actual version: this is invalid.
    private static final String CONFIGURED_DB_VERSION = "999.999";
    static {
        assertThat(ACTUAL_H2_VERSION)
                .as("Test setup - we need the required version to be different from the actual one")
                .doesNotStartWith(CONFIGURED_DB_VERSION);
    }

    private static final String CONFIGURED_DB_VERSION_REPORTED;
    static {
        // For some reason Hibernate ORM infers a micro version of 0; no big deal.
        CONFIGURED_DB_VERSION_REPORTED = CONFIGURED_DB_VERSION + ".0";
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsManifestResource(new StringAsset(loadResourceAndReplacePlaceholders(
                            "META-INF/some-persistence-with-h2-version-placeholder.xml",
                            Map.of("H2_VERSION", "999.999"))),
                            "persistence.xml"))
            .withConfigurationResource("application-datasource-only.properties")
            .assertException(throwable -> assertThat(throwable)
                    .rootCause()
                    .hasMessageContainingAll(
                            "Persistence unit 'templatePU' was configured to run with a database version"
                                    + " of at least '" + CONFIGURED_DB_VERSION_REPORTED + "', but the actual version is '"
                                    + ACTUAL_H2_VERSION + "'",
                            "Consider upgrading your database",
                            "Alternatively, rebuild your application with 'jakarta.persistence.database-product-version="
                                    + ACTUAL_H2_VERSION + "'",
                            "this may disable some features and/or impact performance negatively"));

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    @Test
    public void test() {
        Assertions.fail("Bootstrap should have failed");
    }
}
