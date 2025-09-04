package io.quarkus.hibernate.orm.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ConfigActiveFalseStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.active", "false")
            .assertException(e -> assertThat(e)
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' was deactivated through configuration properties",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the persistence unit, set configuration property 'quarkus.hibernate-orm.active'"
                                    + " to 'true' and configure datasource '<default>'.",
                            "Refer to https://quarkus.io/guides/datasource for guidance.",
                            "This bean is injected into",
                            MyBean.class.getName() + "#session"));

    @Inject
    MyBean myBean;

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
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
