package io.quarkus.hibernate.orm.envers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
