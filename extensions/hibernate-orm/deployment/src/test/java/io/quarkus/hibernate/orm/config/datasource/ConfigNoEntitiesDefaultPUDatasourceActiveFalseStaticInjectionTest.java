package io.quarkus.hibernate.orm.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.assertj.core.api.Assertions;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigNoEntitiesDefaultPUDatasourceActiveFalseStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.active", "false")
            .assertException(e -> assertThat(e)
                    // Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' was deactivated automatically because it doesn't include any entity type and its datasource '<default>' was deactivated",
                            "Datasource '<default>' was deactivated through configuration properties",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.active'"
                                    + " to 'true' and configure datasource '<default>'",
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
            session.createNativeQuery("select 1", Long.class).uniqueResult();
        }
    }
}
