package io.quarkus.jdbc.runtime.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class RuntimeResolvedDriverMissingConfigTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "runtime")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:runtime-missing-driver")
            .assertException(t -> assertThat(t)
                    .hasStackTraceContaining("does not configure the JDBC driver class to load")
                    .hasStackTraceContaining("quarkus.datasource.jdbc-runtime.driver"));

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void startupShouldFail() {
        Assertions.fail("Startup should have failed");
    }
}
