package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InjectableInstance;
import io.quarkus.test.QuarkusUnitTest;

public class NoDatasourceTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            // Ideally we would not add quarkus-reactive-pg-client to the classpath and there _really_ wouldn't be a datasource,
            // but that's inconvenient given our testing setup,
            // so we'll just disable the implicit datasource.
            .overrideConfigKey("quarkus.datasource.reactive", "false");

    @Inject
    InjectableInstance<Mutiny.SessionFactory> sessionFactory;

    @Test
    public void test() {
        // Unlike Hibernate ORM, Hibernate Reactive will silently disable itself if the default datasource is missing, even if there are entities.
        // We may want to revisit that someday, but it's not easy to do without deeper interaction between the Hibernate ORM and Reactive extensions.
        assertThat(sessionFactory.isUnsatisfied()).isTrue();
    }

}
