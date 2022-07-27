package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.context.control.ActivateRequestContext;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigEnabledFalseAndEntityTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class).addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            // This should disable Hibernate Reactive even if there is an entity
            .overrideConfigKey("quarkus.hibernate-orm.enabled", "false");

    @Test
    public void sessionFactory() {
        // The bean is not defined during static init, so it's null.
        assertThat(Arc.container().instance(Mutiny.SessionFactory.class).get())
                .isNull();
    }

    @Test
    @ActivateRequestContext
    public void session() {
        // The bean is not defined during static init, so it's null.
        assertThat(Arc.container().instance(Mutiny.Session.class).get())
                .isNull();
    }
}
