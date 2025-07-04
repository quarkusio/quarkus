package io.quarkus.hibernate.reactive.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.CreationException;
import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.smallrye.mutiny.Uni;

public class ConfigActiveFalseStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.active", "false");

    @Inject
    MyBean myBean;

    @Test
    @RunOnVertxContext
    public void test() {
        assertThatThrownBy(() -> myBean.useHibernate())
                .isInstanceOf(CreationException.class)
                .hasMessageContainingAll(
                        "Cannot retrieve the Mutiny.SessionFactory for persistence unit <default>",
                        "Hibernate Reactive was deactivated through configuration properties");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        Mutiny.SessionFactory sessionFactory;

        public Uni<MyEntity> useHibernate() {
            return sessionFactory.withTransaction(s -> s.find(MyEntity.class, 1L));
        }
    }

}
