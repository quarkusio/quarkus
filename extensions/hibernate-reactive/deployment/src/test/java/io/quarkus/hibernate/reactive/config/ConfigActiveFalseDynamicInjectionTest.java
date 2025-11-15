package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.active", "false");

    @Test
    public void mutinySessionFactory() {
        Mutiny.SessionFactory sessionFactory = Arc.container().instance(Mutiny.SessionFactory.class).get();

        // The bean is always available to be injected during static init
        // since we don't know whether Hibernate Reactive will be active at runtime.
        // So the bean cannot be null.
        assertThat(sessionFactory).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(sessionFactory::getMetamodel)
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "class=org.hibernate.reactive.mutiny.Mutiny$SessionFactory,",
                        "Persistence unit '<default>' was deactivated through configuration properties",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the persistence unit, set configuration property 'quarkus.hibernate-orm.active'"
                                + " to 'true' and configure datasource '<default>'.",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }

}
