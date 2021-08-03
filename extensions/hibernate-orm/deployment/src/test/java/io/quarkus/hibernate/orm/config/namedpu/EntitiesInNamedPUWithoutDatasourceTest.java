package io.quarkus.hibernate.orm.config.namedpu;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class EntitiesInNamedPUWithoutDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .assertException(t -> {
                assertThat(t)
                        .isInstanceOf(ConfigurationException.class)
                        .hasMessageContainingAll("Datasource must be defined for persistence unit 'pu-1'.");
            })
            .withConfigurationResource("application-named-pu-no-datasource.properties")
            .overrideConfigKey("quarkus.datasource.devservices", "false")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addPackage(MyEntity.class.getPackage().getName()));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

}
