package io.quarkus.hibernate.reactive.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigNoEntitiesNamedPUDatasourceActiveFalseDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withEmptyApplication()
            .withConfigurationResource("application-config-named-pu.properties")
            .overrideConfigKey("quarkus.datasource.\"mydatasource\".active", "false");

    @Inject
    @PersistenceUnit("mypu")
    InjectableInstance<Mutiny.SessionFactory> sessionFactory;

    @Test
    public void sessionFactory() {
        doTest(sessionFactory, Mutiny.SessionFactory::getCriteriaBuilder);
    }

    private <T> void doTest(InjectableInstance<T> instance, Consumer<T> action) {
        // The bean is always available to be injected during static init
        // since we don't know whether it will be active at runtime.
        // So the bean proxy cannot be null.
        assertThat(instance.getHandle().getBean())
                .isNotNull()
                .returns(false, InjectableBean::isActive);
        var object = instance.get();
        assertThat(object).isNotNull();
        // However, any attempt to use it at runtime will fail.
        assertThatThrownBy(() -> action.accept(object))
                .isInstanceOf(InactiveBeanException.class)
                .hasMessageContainingAll(
                        "Persistence unit 'mypu' was deactivated automatically because it doesn't include any entity type and its datasource 'mydatasource' was deactivated",
                        "Datasource 'mydatasource' was deactivated through configuration properties",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.\"mydatasource\".active'"
                                + " to 'true' and configure datasource 'mydatasource'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }
}
