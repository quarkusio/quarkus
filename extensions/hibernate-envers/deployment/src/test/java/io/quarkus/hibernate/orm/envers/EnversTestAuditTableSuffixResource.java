package io.quarkus.hibernate.orm.envers;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.internal.SessionImpl;

@Path("/audit-table-suffix")
@ApplicationScoped
public class EnversTestAuditTableSuffixResource {

    @Inject
    EntityManager em;

    @Inject
    @ConfigProperty(name = "quarkus.hibernate-envers.audit-table-suffix")
    String configuredSuffix;

    @GET
    public String getAuditTableName() {
        AuditEntitiesConfiguration auditEntitiesConfiguration = ((((SessionImpl) em.getDelegate())
                .getFactory().getServiceRegistry()).getParentServiceRegistry())
                .getService(EnversService.class).getAuditEntitiesConfiguration();

        String calculatedAuditTableName = auditEntitiesConfiguration.getAuditTableName("entity", "table");
        String expectedAuditTableName = "table" + configuredSuffix;
        if (expectedAuditTableName.equals(calculatedAuditTableName)) {
            return "OK";
        }
        return "Obtained audit table name " + calculatedAuditTableName + " is not same as expected: " + expectedAuditTableName;
    }
}
