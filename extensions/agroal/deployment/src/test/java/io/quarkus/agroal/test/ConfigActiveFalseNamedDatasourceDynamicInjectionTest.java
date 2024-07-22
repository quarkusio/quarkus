package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import javax.sql.DataSource;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseNamedDatasourceDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.datasource.users.active", "false")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.users.db-kind", "h2");

    @Inject
    @io.quarkus.agroal.DataSource("users")
    InjectableInstance<DataSource> dataSource;

    @Inject
    @io.quarkus.agroal.DataSource("users")
    InjectableInstance<AgroalDataSource> agroalDataSource;

    @Test
    public void dataSource() {
        doTest(dataSource);
    }

    @Test
    public void agroalDataSource() {
        doTest(agroalDataSource);
    }

    private void doTest(InjectableInstance<? extends DataSource> instance) {
        // The bean is always available to be injected during static init
        // since we don't know whether the datasource will be active at runtime.
        // So the bean cannot be null.
        var ds = instance.get();
        assertThat(ds).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> ds.getConnection())
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(ConfigurationException.class)
                .hasMessageContainingAll("Datasource 'users' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime, for instance by deactivating consumers (persistence units, ...).",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"users\".active'"
                                + " to 'true' and configure datasource 'users'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }
}
