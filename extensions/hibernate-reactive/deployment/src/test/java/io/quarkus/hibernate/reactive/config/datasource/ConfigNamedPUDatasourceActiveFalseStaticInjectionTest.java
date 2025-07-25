package io.quarkus.hibernate.reactive.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.config.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigNamedPUDatasourceActiveFalseStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application-config-named-pu.properties")
            .overrideConfigKey("quarkus.datasource.\"mydatasource\".active", "false")
            .assertException(e -> assertThat(e)
                    // Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Persistence unit 'mypu' was deactivated automatically because its datasource 'mydatasource' was deactivated",
                            "Datasource 'mydatasource' was deactivated through configuration properties",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.\"mydatasource\".active'"
                                    + " to 'true' and configure datasource 'mydatasource'",
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
        @PersistenceUnit("mypu")
        Mutiny.SessionFactory sessionFactory;

        public void useHibernate() {
            sessionFactory.withTransaction(s -> s.find(MyEntity.class, 1L));
        }
    }
}
