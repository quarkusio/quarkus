package io.quarkus.spring.data.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * An "import.sql" file enables the deprecated behavior of "quarkus.hibernate-orm.sql-load-script",
 * so having it next to a "data.sql" file is ambiguous.
 */
public class BothImportAndDataSqlTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("users1.sql", "data.sql")
                    .addAsResource("users2.sql", "import.sql")
                    .addClasses(User.class, LoginEvent.class, UserRepository.class))
            .withConfigurationResource("application.properties")
            .assertException(t -> assertThat(t)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContainingAll(
                            "(the file 'import.sql' exists in the classpath)",
                            "(the file 'data.sql' exists in the classpath)",
                            "which is ambiguous"));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }
}
