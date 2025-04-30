package io.quarkus.hibernate.orm.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableInstance;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.config.namedpu.MyEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;

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
            super("pu-1", "pu-2");
        }
    }

    public static class Pu2ActiveTest extends MultiplePUAsAlternativesWithBeanProducerTest {
        @RegisterExtension
        static QuarkusUnitTest runner = runner("pu-2", "ds-2");

        public Pu2ActiveTest() {
            super("pu-2", "pu-1");
        }
    }

    static QuarkusUnitTest runner(String activePuName, String activeDsName) {
        return new QuarkusUnitTest()
                .withApplicationRoot((jar) -> jar
                        .addPackage(MyEntity.class.getPackage().getName())
                        .addClass(MyProducer.class))
                .overrideConfigKey("quarkus.hibernate-orm.pu-1.packages", MyEntity.class.getPackageName())
                .overrideConfigKey("quarkus.hibernate-orm.pu-1.datasource", "ds-1")
                .overrideConfigKey("quarkus.hibernate-orm.pu-1.schema-management.strategy", "drop-and-create")
                .overrideConfigKey("quarkus.hibernate-orm.pu-1.active", "false")
                .overrideConfigKey("quarkus.datasource.ds-1.db-kind", "h2")
                .overrideConfigKey("quarkus.datasource.ds-1.active", "false")
                .overrideConfigKey("quarkus.hibernate-orm.pu-2.packages", MyEntity.class.getPackageName())
                .overrideConfigKey("quarkus.hibernate-orm.pu-2.datasource", "ds-2")
                .overrideConfigKey("quarkus.hibernate-orm.pu-2.schema-management.strategy", "drop-and-create")
                .overrideConfigKey("quarkus.hibernate-orm.pu-2.active", "false")
                .overrideConfigKey("quarkus.datasource.ds-2.db-kind", "h2")
                .overrideConfigKey("quarkus.datasource.ds-2.active", "false")
                // This is where we select the active PU / datasource
                .overrideRuntimeConfigKey("quarkus.hibernate-orm." + activePuName + ".active", "true")
                .overrideRuntimeConfigKey("quarkus.datasource." + activeDsName + ".active", "true")
                .overrideRuntimeConfigKey("quarkus.datasource." + activeDsName + ".jdbc.url", "jdbc:h2:mem:testds1");
    }

    private final String activePuName;
    private final String inactivePuName;

    protected MultiplePUAsAlternativesWithBeanProducerTest(String activePuName, String inactivePuName) {
        this.activePuName = activePuName;
        this.inactivePuName = inactivePuName;
    }

    @Inject
    Session customIndirectSessionBean;

    @Test
    public void testExplicitSessionBeanUsable() {
        doTestPersistRetrieve(Arc.container()
                .select(Session.class, new PersistenceUnit.PersistenceUnitLiteral(activePuName)).get(),
                1L);
    }

    @Test
    public void testCustomIndirectSessionBeanUsable() {
        doTestPersistRetrieve(customIndirectSessionBean, 2L);
    }

    @Test
    public void testInactiveSessionBeanUnusable() {
        QuarkusTransaction.requiringNew().run(() -> {
            assertThatThrownBy(() -> Arc.container()
                    .select(Session.class, new PersistenceUnit.PersistenceUnitLiteral(inactivePuName)).get()
                    .find(MyEntity.class, 3L))
                    .hasMessageContainingAll(
                            "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit " + inactivePuName,
                            "Hibernate ORM was deactivated through configuration properties");
        });
    }

    private static void doTestPersistRetrieve(Session session, long id) {
        QuarkusTransaction.requiringNew().run(() -> {
            MyEntity entity = new MyEntity();
            entity.setId(id);
            entity.setName("text" + id);
            session.persist(entity);
        });
        QuarkusTransaction.requiringNew().run(() -> {
            MyEntity entity = session.get(MyEntity.class, id);
            assertThat(entity.getName()).isEqualTo("text" + id);
        });
    }

    private static class MyProducer {
        @Inject
        @DataSource("ds-1")
        InjectableInstance<AgroalDataSource> dataSource1Bean;

        @Inject
        @DataSource("ds-2")
        InjectableInstance<AgroalDataSource> dataSource2Bean;

        @Inject
        @PersistenceUnit("pu-1")
        Session pu1SessionBean;

        @Inject
        @PersistenceUnit("pu-2")
        Session pu2SessionBean;

        @Produces
        @ApplicationScoped
        public Session session() {
            if (dataSource1Bean.getHandle().getBean().isActive()) {
                return pu1SessionBean;
            } else if (dataSource2Bean.getHandle().getBean().isActive()) {
                return pu2SessionBean;
            } else {
                throw new RuntimeException("No active datasource!");
            }
        }
    }
}
