package io.quarkus.jdbc.runtime.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.QuarkusExtensionTest;

public class RuntimeResolvedDriverWrongTypeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey("quarkus.datasource.db-kind", "runtime")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:runtime-wrong-type")
            .overrideRuntimeConfigKey("quarkus.datasource.jdbc-runtime.driver", "java.lang.String")
            .assertException(t -> assertThat(t)
                    .hasStackTraceContaining("java.lang.String")
                    .hasStackTraceContaining("is neither an implementation of"));

    @Inject
    AgroalDataSource dataSource;

    @Test
    public void startupShouldFail() {
        Assertions.fail("Startup should have failed");
    }
}
