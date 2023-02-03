package io.quarkus.hibernate.orm.envers;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

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
    EntityManagerFactory emf1;

    @Inject
    @PersistenceUnit("db2")
    EntityManagerFactory emf2;

    @Test
    public void testTableName() {
        String generatedTableName = getAuditConfiguration(emf).getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("P_table");

        generatedTableName = getAuditConfiguration(emf1).getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("T_table");

        generatedTableName = getAuditConfiguration(emf2).getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("R_table");
    }

    @Test
    public void testRevisionFieldName() {
        String configuredRevisionFieldName = getAuditConfiguration(emf).getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("GEN");

        configuredRevisionFieldName = getAuditConfiguration(emf1).getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("REVISION");

        configuredRevisionFieldName = getAuditConfiguration(emf2).getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("REV");
    }

    @Test
    public void testRevisionTypeName() {
        String configuredRevisionTypeName = getAuditConfiguration(emf).getRevisionTypePropName();
        assertThat(configuredRevisionTypeName).isEqualTo("GEN_TYPE");

        configuredRevisionTypeName = getAuditConfiguration(emf1).getRevisionTypePropName();
        assertThat(configuredRevisionTypeName).isEqualTo("REV_TYPE");

        configuredRevisionTypeName = getAuditConfiguration(emf2).getRevisionTypePropName();
        assertThat(configuredRevisionTypeName).isEqualTo("REVTYPE");
    }

    private AuditEntitiesConfiguration getAuditConfiguration(EntityManagerFactory emf) {
        return ((((SessionFactoryImplementor) emf
                .unwrap(SessionFactoryImpl.class))
                .getServiceRegistry()).getParentServiceRegistry())
                .getService(EnversService.class).getAuditEntitiesConfiguration();
    }
}
