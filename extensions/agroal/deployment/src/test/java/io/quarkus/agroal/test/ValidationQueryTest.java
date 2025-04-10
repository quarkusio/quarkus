package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class ValidationQueryTest {

    private static final String testLogPath = "target/validation-query-test.log";

    //tag::injection[]
    @Inject
    AgroalDataSource defaultDataSource;
    //end::injection[]

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("application-datasource-with-validation.properties")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url",
                    "jdbc:h2:tcp://localhost/mem:default?queryLog=%s;DATABASE_EVENT_LISTENER=io.quarkus.agroal.test.QueryLoggingH2DBEventListener" //Register QueryLoggingH2DBEventListener
                            .formatted(testLogPath));

    @Test
    public void testQueryTimeoutIsApplied() throws SQLException {

        //Test connection is acquirable
        try (Connection connection = defaultDataSource.getConnection()) {
            //nop
        }
        assertThat(new File(testLogPath)).content().contains("SET QUERY_TIMEOUT ?");
    }
}
