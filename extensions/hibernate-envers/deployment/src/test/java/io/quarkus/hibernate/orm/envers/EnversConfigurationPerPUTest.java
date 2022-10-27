package io.quarkus.hibernate.orm.envers;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusUnitTest;

public class EnversConfigurationPerPUTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAuditedEntity.class))
            .withConfigurationResource("application-multiple-pu.properties");

    @Inject
    EntityManagerFactory emf;

    @Inject
    @PersistenceUnit("db1")
    EntityManager em1;

    @Inject
    @PersistenceUnit("db2")
    EntityManager em2;

    @Test
    public void testTableName() {
        String generatedTableName = getAuditConfiguration().getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("P_table");
    }

    @Test
    public void testRevisionFieldName() {
        String configuredRevisionFieldName = getAuditConfiguration().getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("GEN");
    }

    @Test
    public void testRevisionTypeName() {
        String configuredRevisionTypeName = getAuditConfiguration().getRevisionTypePropName();
        assertThat(configuredRevisionTypeName).isEqualTo("GEN_TYPE");
    }

    private AuditEntitiesConfiguration getAuditConfiguration() {
        return ((((SessionFactoryImplementor) emf
                .unwrap(SessionFactoryImpl.class))
                .getServiceRegistry()).getParentServiceRegistry())
                .getService(EnversService.class).getAuditEntitiesConfiguration();
    }
}
