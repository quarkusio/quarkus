package io.quarkus.hibernate.orm.envers;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
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
    EntityManagerFactory emf1;

    @Inject
    @PersistenceUnit("db2")
    EntityManagerFactory emf2;

    @Test
    public void testTableName() {
        String generatedTableName = getAuditConfiguration("default").getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("P_table");

        generatedTableName = getAuditConfiguration("db1").getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("T_table");

        generatedTableName = getAuditConfiguration("db2").getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("R_table");
    }

    @Test
    public void testRevisionFieldName() {
        String configuredRevisionFieldName = getAuditConfiguration("default").getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("GEN");

        configuredRevisionFieldName = getAuditConfiguration("db1").getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("REVISION");

        configuredRevisionFieldName = getAuditConfiguration("db2").getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("REV");
    }

    @Test
    public void testRevisionTypeName() {
        String configuredRevisionTypeName = getAuditConfiguration("default").getRevisionTypePropName();
        assertThat(configuredRevisionTypeName).isEqualTo("GEN_TYPE");

        configuredRevisionTypeName = getAuditConfiguration("db1").getRevisionTypePropName();
        assertThat(configuredRevisionTypeName).isEqualTo("REV_TYPE");

        configuredRevisionTypeName = getAuditConfiguration("db2").getRevisionTypePropName();
        assertThat(configuredRevisionTypeName).isEqualTo("REVTYPE");
    }

    private AuditEntitiesConfiguration getAuditConfiguration(String auditConfig) {
        switch (auditConfig) {
            case "db1":
                return ((((SessionFactoryImplementor) emf1
                        .unwrap(SessionFactoryImpl.class))
                        .getServiceRegistry()).getParentServiceRegistry())
                        .getService(EnversService.class).getAuditEntitiesConfiguration();
            case "db2":
                return ((((SessionFactoryImplementor) emf2
                        .unwrap(SessionFactoryImpl.class))
                        .getServiceRegistry()).getParentServiceRegistry())
                        .getService(EnversService.class).getAuditEntitiesConfiguration();
            default:
                return ((((SessionFactoryImplementor) emf
                        .unwrap(SessionFactoryImpl.class))
                        .getServiceRegistry()).getParentServiceRegistry())
                        .getService(EnversService.class).getAuditEntitiesConfiguration();
        }
    }
}
