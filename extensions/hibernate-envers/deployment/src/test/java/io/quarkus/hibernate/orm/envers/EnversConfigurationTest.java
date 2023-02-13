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

import io.quarkus.test.QuarkusUnitTest;

public class EnversConfigurationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyAuditedEntity.class))
            .withConfigurationResource("application-emptysuffixtable.properties");

    @Inject
    EntityManagerFactory emf;

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
