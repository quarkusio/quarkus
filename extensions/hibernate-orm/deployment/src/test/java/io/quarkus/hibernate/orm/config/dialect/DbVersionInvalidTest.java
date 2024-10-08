package io.quarkus.hibernate.orm.config.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.test.QuarkusUnitTest;

public class DbVersionInvalidTest {

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
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.db-version", "999.999")
            .assertException(throwable -> assertThat(throwable)
                    .rootCause()
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' was configured to run with a database version"
                                    + " of at least '" + CONFIGURED_DB_VERSION_REPORTED + "', but the actual version is '"
                                    + ACTUAL_H2_VERSION + "'",
                            "Consider upgrading your database",
                            "Alternatively, rebuild your application with 'quarkus.datasource.db-version="
                                    + ACTUAL_H2_VERSION + "'",
                            "this may disable some features and/or impact performance negatively",
                            "disable the check with 'quarkus.hibernate-orm.database.version-check.enabled=false'"));

    @Inject
    SessionFactory sessionFactory;

    @Inject
    Session session;

    @Test
    public void test() {
        Assertions.fail("Bootstrap should have failed");
    }
}
