package io.quarkus.hibernate.orm.envers;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.Configuration;
import org.hibernate.internal.SessionFactoryImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.test.QuarkusUnitTest;

public class EnversConfigurationPerPUTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(MyAuditedEntity.class))
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
        String generatedTableName = getConfiguration(emf).getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("P_table");

        generatedTableName = getConfiguration(emf1).getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("T_table");

        generatedTableName = getConfiguration(emf2).getAuditTableName("entity", "table");
        assertThat(generatedTableName).isEqualTo("R_table");
    }

    @Test
    public void testRevisionFieldName() {
        String configuredRevisionFieldName = getConfiguration(emf).getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("GEN");

        configuredRevisionFieldName = getConfiguration(emf1).getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("REVISION");

        configuredRevisionFieldName = getConfiguration(emf2).getRevisionFieldName();
        assertThat(configuredRevisionFieldName).isEqualTo("REV");
    }

    @Test
    public void testRevisionTypeName() {
        String configuredRevisionTypeName = getConfiguration(emf).getRevisionTypePropertyName();
        assertThat(configuredRevisionTypeName).isEqualTo("GEN_TYPE");

        configuredRevisionTypeName = getConfiguration(emf1).getRevisionTypePropertyName();
        assertThat(configuredRevisionTypeName).isEqualTo("REV_TYPE");

        configuredRevisionTypeName = getConfiguration(emf2).getRevisionTypePropertyName();
        assertThat(configuredRevisionTypeName).isEqualTo("REVTYPE");
    }

    private Configuration getConfiguration(EntityManagerFactory emf) {
        return ((((SessionFactoryImplementor) emf.unwrap(SessionFactoryImpl.class)).getServiceRegistry())
                .getParentServiceRegistry()).getService(EnversService.class).getConfig();
    }
}
