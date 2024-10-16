package io.quarkus.agroal.test;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.Arc;
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
        var instanceHandle = container.instance(DataSources.class);
        // Check that the following call won't trigger a lazy initialization:
        // the DataSources bean must be eagerly initialized.
        assertThat(container.getActiveContext(Singleton.class).getState()
                .getContextualInstances().get(instanceHandle.getBean()))
                .as("Eagerly instantiated DataSources bean")
                .isNotNull();
        // Check that the datasource has already been eagerly created.
        assertThat(instanceHandle.get().isDataSourceCreated(DataSourceUtil.DEFAULT_DATASOURCE_NAME))
                .isTrue();
    }

}
