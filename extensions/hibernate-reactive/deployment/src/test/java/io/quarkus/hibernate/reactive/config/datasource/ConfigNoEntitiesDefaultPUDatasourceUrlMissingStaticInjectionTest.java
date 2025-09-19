package io.quarkus.hibernate.reactive.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class ConfigNoEntitiesDefaultPUDatasourceUrlMissingStaticInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.datasource.reactive.url", "")
            // The URL won't be missing if dev services are enabled
            .overrideConfigKey("quarkus.devservices.enabled", "false")
            .assertException(e -> assertThat(e)
                    // Can't use isInstanceOf due to weird classloading in tests
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' was deactivated automatically because it doesn't include any entity type and its datasource '<default>' was deactivated",
                            "Datasource '<default>' was deactivated automatically because its URL is not set.",
                            "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                            "To activate the datasource, set configuration property 'quarkus.datasource.reactive.url'.",
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
        Mutiny.SessionFactory sessionFactory;

        public Uni<Long> useHibernate() {
            return sessionFactory.withTransaction(s -> s.createNativeQuery("select 1", Long.class).getSingleResult());
        }
    }
}
