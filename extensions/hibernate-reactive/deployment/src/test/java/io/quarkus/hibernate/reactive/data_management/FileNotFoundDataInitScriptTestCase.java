package io.quarkus.hibernate.reactive.data_management;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

public class FileNotFoundDataInitScriptTestCase {
    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.data-management.init-script", "file-that-does-not-exist.sql")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll("Unable to find file referenced in '"
                            + "quarkus.hibernate-orm.data-management.init-script=file-that-does-not-exist.sql'. Remove property or add file to your path."));

    @Test
    public void testDataInitScriptFileAbsentTest() {
        // deployment exception should happen first
        Assertions.fail();
    }

    @Entity
    public static class MyEntity {

        @Id
        public long id;

        public String name;

        public MyEntity() {
        }

        public MyEntity(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ":" + name;
        }
    }
}
