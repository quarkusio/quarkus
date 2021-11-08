package io.quarkus.hibernate.orm.envers;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.envers.boot.internal.EnversService;
import org.hibernate.envers.configuration.internal.AuditEntitiesConfiguration;
import org.hibernate.internal.SessionFactoryImpl;

@Path("/audit-table-empty-suffix")
@ApplicationScoped
public class EnversTestEmptyTableSuffixResource {

    @Inject
    EntityManagerFactory emf;

    @GET
    public String getAuditTableName() {
        AuditEntitiesConfiguration auditEntitiesConfiguration = ((((SessionFactoryImplementor) emf
                .unwrap(SessionFactoryImpl.class))
                        .getServiceRegistry()).getParentServiceRegistry())
                                .getService(EnversService.class).getAuditEntitiesConfiguration();

        String calculatedAuditTableName = auditEntitiesConfiguration.getAuditTableName("entity", "table");
        if ("P_table".equals(calculatedAuditTableName)) {
            return "OK";
        }
        return "Obtained audit table name '" + calculatedAuditTableName + "' is not same as expected: 'P_table'";
    }
}
