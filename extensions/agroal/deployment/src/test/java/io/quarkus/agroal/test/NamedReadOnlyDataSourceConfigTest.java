package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that {@code quarkus.datasource."name".jdbc.read-only} is applied for named datasources.
 */
public class NamedReadOnlyDataSourceConfigTest {

    @Inject
    @DataSource("testing")
    AgroalDataSource testingDataSource;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withConfigurationResource("application-named-datasource.properties")
            .overrideConfigKey("quarkus.datasource.testing.jdbc.read-only", "true");

    @Test
    public void testNamedDataSourceIsReadOnly() {
        assertThat(testingDataSource.getConfiguration()
                .connectionPoolConfiguration()
                .connectionFactoryConfiguration()
                .readOnly()).isTrue();
    }
}
