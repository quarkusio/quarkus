package io.quarkus.hibernate.reactive.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.config.MyEntity;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;

/**
 * Tests a use case where multiple PU/datasources are defined at build time,
 * but only one is used at runtime.
 * <p>
 * This is mostly useful when each datasource has a distinct db-kind, but in theory that shouldn't matter,
 * so we use the h2 db-kind everywhere here to keep test dependencies simpler.
 */
public abstract class MultiplePUAsAlternativesWithBeanProducerTest {

    public static class Pu1ActiveTest extends MultiplePUAsAlternativesWithBeanProducerTest {
        @RegisterExtension
        static QuarkusUnitTest runner = runner("pu-1", "ds-1");

        public Pu1ActiveTest() {
            super("pu-1", "pu-2", "ds-2");
        }
    }

    public static class Pu2ActiveTest extends MultiplePUAsAlternativesWithBeanProducerTest {
        @RegisterExtension
        static QuarkusUnitTest runner = runner("pu-2", "ds-2");

        public Pu2ActiveTest() {
            super("pu-2", "pu-1", "ds-1");
        }
    }

    static QuarkusUnitTest runner(String activePuName, String activeDsName) {
        return new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addPackage(MyEntity.class.getPackage().getName())
                        .addClass(MyProducer.class))
                .withConfigurationResource("application-postgres-reactive-url-only.properties")
                .overrideConfigKey("quarkus.hibernate-orm.pu-1.packages", MyEntity.class.getPackageName())
                .overrideConfigKey("quarkus.hibernate-orm.pu-1.datasource", "ds-1")
                .overrideConfigKey("quarkus.hibernate-orm.pu-1.schema-management.strategy", "drop-and-create")
                .overrideConfigKey("quarkus.hibernate-orm.pu-1.active", "false")
                .overrideConfigKey("quarkus.datasource.ds-1.db-kind", "postgresql")
                .overrideConfigKey("quarkus.datasource.ds-1.active", "false")
                .overrideConfigKey("quarkus.hibernate-orm.pu-2.packages", MyEntity.class.getPackageName())
                .overrideConfigKey("quarkus.hibernate-orm.pu-2.datasource", "ds-2")
                .overrideConfigKey("quarkus.hibernate-orm.pu-2.schema-management.strategy", "drop-and-create")
                .overrideConfigKey("quarkus.hibernate-orm.pu-2.active", "false")
                .overrideConfigKey("quarkus.datasource.ds-2.db-kind", "postgresql")
                .overrideConfigKey("quarkus.datasource.ds-2.active", "false")
                // This is where we select the active PU / datasource
                .overrideRuntimeConfigKey("quarkus.hibernate-orm." + activePuName + ".active", "true")
                .overrideRuntimeConfigKey("quarkus.datasource." + activeDsName + ".active", "true")
                .overrideRuntimeConfigKey("quarkus.datasource." + activeDsName + ".reactive.url", "${postgres.reactive.url}");
    }

    private final String activePuName;
    private final String inactivePuName;
    private final String inactiveDsName;

    protected MultiplePUAsAlternativesWithBeanProducerTest(String activePuName, String inactivePuName, String inactiveDsName) {
        this.activePuName = activePuName;
        this.inactivePuName = inactivePuName;
        this.inactiveDsName = inactiveDsName;
    }

    @Inject
    Mutiny.SessionFactory customIndirectSessionFactoryBean;

    @Test
    @RunOnVertxContext
    public void testExplicitSessionFactoryBeanUsable(UniAsserter asserter) {
        doTestPersistRetrieve(asserter, Arc.container()
                .select(Mutiny.SessionFactory.class, new PersistenceUnit.PersistenceUnitLiteral(activePuName)).get(),
                1L);
    }

    @Test
    @RunOnVertxContext
    public void testCustomIndirectSessionFactoryBeanUsable(UniAsserter asserter) {
        doTestPersistRetrieve(asserter, customIndirectSessionFactoryBean, 2L);
    }

    @Test
    @RunOnVertxContext
    public void testInactiveSessionFactoryBeanUnusable(UniAsserter asserter) {
        asserter.assertFailedWith(() -> Arc.container()
                .select(Mutiny.SessionFactory.class, new PersistenceUnit.PersistenceUnitLiteral(inactivePuName)).get()
                .withTransaction(session -> null),
                failure -> assertThat(failure)
                        .hasMessageContainingAll(
                                "Persistence unit '" + inactivePuName + "' was deactivated through configuration properties",
                                "To activate the persistence unit, set configuration property 'quarkus.hibernate-orm.\""
                                        + inactivePuName
                                        + "\".active'"
                                        + " to 'true' and configure datasource '" + inactiveDsName + "'",
                                "Refer to https://quarkus.io/guides/datasource for guidance."));
    }

    private static void doTestPersistRetrieve(UniAsserter asserter, Mutiny.SessionFactory sessionFactory, long id) {
        asserter.execute(() -> sessionFactory.withTransaction(session -> {
            MyEntity entity = new MyEntity();
            entity.setId(id);
            entity.setName("text" + id);
            return session.persist(entity);
        }));

        asserter.assertThat(
                () -> sessionFactory.withTransaction(session -> session.find(MyEntity.class, id)),
                entity -> assertThat(entity.getName()).isEqualTo("text" + id));
    }

    private static class MyProducer {
        @Inject
        @PersistenceUnit("pu-1")
        InjectableInstance<Mutiny.SessionFactory> pu1SessionFactoryBean;

        @Inject
        @PersistenceUnit("pu-2")
        InjectableInstance<Mutiny.SessionFactory> pu2SessionFactoryBean;

        @Produces
        @ApplicationScoped
        public Mutiny.SessionFactory sessionFactory() {
            if (pu1SessionFactoryBean.getHandle().getBean().isActive()) {
                return pu1SessionFactoryBean.get();
            } else if (pu2SessionFactoryBean.getHandle().getBean().isActive()) {
                return pu2SessionFactoryBean.get();
            } else {
                throw new RuntimeException("No active persistence unit!");
            }
        }
    }
}
