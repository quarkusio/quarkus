package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.runtime.AgroalDataSourceUtil;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ClientProxy;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Check that datasources are created eagerly on application startup.
 * <p>
 * This has always been the case historically, so we want to keep it that way.
 */
public class EagerStartupTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withConfigurationResource("base.properties");

    @Test
    public void shouldStartEagerly() {
        var container = Arc.container();
        var instanceHandle = container.instance(AgroalDataSource.class,
                AgroalDataSourceUtil.qualifier(DataSourceUtil.DEFAULT_DATASOURCE_NAME));
        // Check that the datasource has already been eagerly created.
        assertThat(container.getActiveContext(ApplicationScoped.class).getState()
                .getContextualInstances().get(instanceHandle.getBean()))
                .as("Eagerly instantiated DataSource bean")
                .isNotInstanceOf(ClientProxy.class) // Just to be sure I didn't misuse CDI: this should be the actual underlying instance.
                .isNotNull();
    }

}
