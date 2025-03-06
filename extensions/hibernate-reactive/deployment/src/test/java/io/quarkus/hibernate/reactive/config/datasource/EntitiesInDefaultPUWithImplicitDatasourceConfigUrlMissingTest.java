package io.quarkus.hibernate.reactive.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.reactive.config.MyEntity;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class EntitiesInDefaultPUWithImplicitDatasourceConfigUrlMissingTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-reactive-pg-client-deployment", Version.getVersion()), // this triggers Agroal
                    Dependency.of("io.quarkus", "quarkus-jdbc-h2-deployment", Version.getVersion()) // this triggers Agroal
            ))
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "The default datasource must be configured for Hibernate Reactive",
                            "Refer to https://quarkus.io/guides/datasource for guidance."));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }

}
