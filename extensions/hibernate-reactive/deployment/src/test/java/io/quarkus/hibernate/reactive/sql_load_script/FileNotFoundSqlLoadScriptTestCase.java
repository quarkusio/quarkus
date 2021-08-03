package io.quarkus.hibernate.reactive.sql_load_script;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.test.QuarkusUnitTest;

public class FileNotFoundSqlLoadScriptTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.sql-load-script", "file-that-does-not-exist.sql")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationError.class)
                    .hasMessageContainingAll("Unable to find file referenced in '"
                            + "quarkus.hibernate-orm.sql-load-script=file-that-does-not-exist.sql'. Remove property or add file to your path."));

    @Test
    public void testSqlLoadScriptFileAbsentTest() {
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
