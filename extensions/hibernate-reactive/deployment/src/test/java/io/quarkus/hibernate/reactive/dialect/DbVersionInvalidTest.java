package io.quarkus.hibernate.reactive.dialect;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.reactive.DialectUtils;
import io.quarkus.hibernate.reactive.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class DbVersionInvalidTest {

    // We will set the DB version to something higher than the actual version: this is invalid.
    private static final String CONFIGURED_DB_VERSION = "999.999";
    private static final String CONFIGURED_DB_VERSION_REPORTED;
    static {
        // For some reason Hibernate ORM infers a micro version of 0; no big deal.
        CONFIGURED_DB_VERSION_REPORTED = CONFIGURED_DB_VERSION + ".0";
    }

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(DialectUtils.class)
                    .addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.db-version", CONFIGURED_DB_VERSION)
            .assertException(throwable -> assertThat(throwable)
                    .rootCause()
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' was configured to run with a database version"
                                    + " of at least '" + CONFIGURED_DB_VERSION_REPORTED + "', but the actual version is '",
                            "Consider upgrading your database",
                            "Alternatively, rebuild your application with 'quarkus.datasource.db-version=",
                            "this may disable some features and/or impact performance negatively",
                            "disable the check with 'quarkus.hibernate-orm.database.version-check.enabled=false'"));

    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Test
    public void test() {
        Assertions.fail("Bootstrap should have failed");
    }
}
