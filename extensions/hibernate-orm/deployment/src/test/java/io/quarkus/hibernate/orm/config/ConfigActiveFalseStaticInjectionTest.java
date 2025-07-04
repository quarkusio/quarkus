package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.active", "false");

    @Inject
    MyBean myBean;

    @Test
    public void test() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> myBean.useHibernate());
        assertThat(e)
                .hasMessageContainingAll(
                        "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit <default>",
                        "Hibernate ORM was deactivated through configuration properties");
    }

    @ApplicationScoped
    public static class MyBean {
        @Inject
        Session session;

        @Transactional
        public void useHibernate() {
            session.find(MyEntity.class, 1L);
        }
    }
}
