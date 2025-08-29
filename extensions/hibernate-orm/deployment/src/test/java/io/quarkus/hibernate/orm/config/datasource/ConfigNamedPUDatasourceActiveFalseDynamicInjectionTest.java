package io.quarkus.hibernate.orm.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.function.Consumer;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;
import jakarta.persistence.SchemaManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.InactiveBeanException;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.config.MyEntity;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigNamedPUDatasourceActiveFalseDynamicInjectionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .withConfigurationResource("application-config-named-pu.properties")
            .overrideConfigKey("quarkus.datasource.\"mydatasource\".active", "false");

    @Inject
    @PersistenceUnit("mypu")
    InjectableInstance<SessionFactory> sessionFactory;

    @Inject
    @PersistenceUnit("mypu")
    InjectableInstance<Session> session;

    // Just try another bean type, but not all of them...
    @Inject
    @PersistenceUnit("mypu")
    InjectableInstance<SchemaManager> schemaManager;

    @Test
    public void sessionFactory() {
        doTest(sessionFactory, SessionFactory::getCriteriaBuilder);
    }

    @Test
    @ActivateRequestContext
    public void session() {
        doTest(session, s -> s.find(MyEntity.class, 1L));
    }

    @Test
    public void schemaManager() {
        doTest(schemaManager, m -> m.truncate());
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
                        "Persistence unit 'mypu' was deactivated automatically because its datasource 'mydatasource' was deactivated",
                        "Datasource 'mydatasource' was deactivated through configuration properties",
                        "To avoid this exception while keeping the bean inactive", // Message from Arc with generic hints
                        "To activate the datasource, set configuration property 'quarkus.datasource.\"mydatasource\".active'"
                                + " to 'true' and configure datasource 'mydatasource'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }
}
