package io.quarkus.liquibase.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.liquibase.LiquibaseFactory;
import io.quarkus.test.QuarkusUnitTest;

public class LiquibaseExtensionSecureParsingEnabledTest {

    @Inject
    LiquibaseFactory liquibaseFactory;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("insecure-db/changeLog.xml", "db/changeLog.xml")
                    .addAsResource("insecure-db/dbchangelog-3.8.xsd", "db/dbchangelog-3.8.xsd")
                    .addAsResource("secure-parsing-enabled.properties", "application.properties"))
            .assertException(t -> {
                assertThat(t.getCause().getCause())
                        .hasMessageContaining("because 'file' access is not allowed");
            });

    @Test
    public void testSecureParsing() throws Exception {
        fail("should not be executed");
    }
}
