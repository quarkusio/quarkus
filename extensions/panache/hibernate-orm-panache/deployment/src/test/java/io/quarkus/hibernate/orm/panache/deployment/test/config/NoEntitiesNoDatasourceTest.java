package io.quarkus.hibernate.orm.panache.deployment.test.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.test.QuarkusUnitTest;

public class NoEntitiesNoDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withEmptyApplication()
            // Ideally we would not add quarkus-jdbc-h2 to the classpath and there _really_ wouldn't be a datasource,
            // but that's inconvenient given our testing setup,
            // so we'll just disable the implicit datasource.
            .overrideConfigKey("quarkus.datasource.jdbc", "false");

    // Test for https://github.com/quarkusio/quarkus/issues/50247
    // When having no entities, no configuration, and no datasource,
    // we should still be able to start the application.
    @Test
    @Transactional
    public void test() {
        // But Hibernate ORM should be disabled, so its beans should not be there.
        assertThat(Panache.getSession()).isNull();
    }

}
