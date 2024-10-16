package io.quarkus.hibernate.orm.config.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.datasource.runtime.DataSourceSupport;
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
 * <p>
 * See {@link MultiplePUAsAlternativesWithActivePU2Test} for the counterpart where PU2 is used at runtime.
 */
public class MultiplePUAsAlternativesWithActivePU1Test {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addPackage(MyEntity.class.getPackage().getName())
                    .addClass(MyProducer.class))
            .overrideConfigKey("quarkus.hibernate-orm.pu-1.packages", MyEntity.class.getPackageName())
            .overrideConfigKey("quarkus.hibernate-orm.pu-1.datasource", "ds-1")
            .overrideConfigKey("quarkus.hibernate-orm.pu-1.database.generation", "drop-and-create")
            .overrideConfigKey("quarkus.hibernate-orm.pu-1.active", "false")
            .overrideConfigKey("quarkus.datasource.ds-1.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.ds-1.active", "false")
            .overrideConfigKey("quarkus.hibernate-orm.pu-2.packages", MyEntity.class.getPackageName())
            .overrideConfigKey("quarkus.hibernate-orm.pu-2.datasource", "ds-2")
            .overrideConfigKey("quarkus.hibernate-orm.pu-2.database.generation", "drop-and-create")
            .overrideConfigKey("quarkus.hibernate-orm.pu-2.active", "false")
            .overrideConfigKey("quarkus.datasource.ds-2.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.ds-2.active", "false")
            // This is where we select PU1 / datasource 1
            .overrideRuntimeConfigKey("quarkus.hibernate-orm.pu-1.active", "true")
            .overrideRuntimeConfigKey("quarkus.datasource.ds-1.active", "true")
            .overrideRuntimeConfigKey("quarkus.datasource.ds-1.jdbc.url", "jdbc:h2:mem:testds1");

    @Inject
    @PersistenceUnit("pu-1")
    Session explicitSessionBean;

    @Inject
    Session customIndirectSessionBean;

    @Inject
    @PersistenceUnit("pu-2")
    Session inactiveSessionBean;

    @Test
    public void testExplicitSessionBeanUsable() {
        doTestPersistRetrieve(explicitSessionBean, 1L);
    }

    @Test
    public void testCustomIndirectSessionBeanUsable() {
        doTestPersistRetrieve(customIndirectSessionBean, 2L);
    }

    @Test
    public void testInactiveSessionBeanUnusable() {
        QuarkusTransaction.requiringNew().run(() -> {
            assertThatThrownBy(() -> inactiveSessionBean.find(MyEntity.class, 3L))
                    .hasMessageContainingAll(
                            "Cannot retrieve the EntityManagerFactory/SessionFactory for persistence unit pu-2",
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
        DataSourceSupport dataSourceSupport;

        @Inject
        @PersistenceUnit("pu-1")
        Session pu1SessionBean;

        @Inject
        @PersistenceUnit("pu-2")
        Session pu2SessionBean;

        @Produces
        @ApplicationScoped
        public Session session() {
            if (dataSourceSupport.getInactiveNames().contains("ds-1")) {
                return pu2SessionBean;
            } else {
                return pu1SessionBean;
            }
        }
    }
}
