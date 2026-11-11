package io.quarkus.hibernate.orm.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.MyEntity;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class JPATestDdlRequiredNoValidatorTestCase {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-hibernate-validator"),
                    ArtifactKey.of("io.quarkus", "quarkus-hibernate-validator-deployment"),
                    ArtifactKey.of("org.hibernate.validator", "hibernate-validator")))
            .overrideConfigKey("quarkus.hibernate-orm.validation.mode", "none")
            .overrideConfigKey("quarkus.hibernate-orm.validation.ddl-influence", "required")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "ddl-influence", "required", "no Jakarta Validation provider is present"));

    @Test
    void startupMustFailWithoutValidationProvider() {
        Assertions.fail("ddl-influence=required must reject startup without a Jakarta Validation provider");
    }
}