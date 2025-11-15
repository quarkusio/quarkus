package io.quarkus.hibernate.orm.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.DefaultEntity;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.orm.multiplepersistenceunits.model.config.user.User;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class MultiplePersistenceUnitsInconsistentStorageEnginesTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(User.class)
                    .addClass(DefaultEntity.class)
                    .addClass(User.class)
                    .addClass(Plane.class)
                    .addAsResource("application-multiple-persistence-units-inconsistent-storage-engines.properties",
                            "application.properties"))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-jdbc-mysql-deployment", Version.getVersion())))
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining(
                            "The dialect storage engine is a global configuration property: it must be consistent across all persistence units."));

    @Test
    public void testInvalidConfiguration() {
        // deployment exception should happen first
        Assertions.fail();
    }
}
