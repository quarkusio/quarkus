
package io.quarkus.hibernate.orm.envers.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.envers.strategy.AuditStrategy;
import org.hibernate.envers.strategy.ValidityAuditStrategy;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-audit-strategy")
@ApplicationScoped
public class EnversTestAuditStrategyResource extends AbstractEnversResource {
    @GET
    public String getConfiguredAuditStrategy() {
        final AuditStrategy auditStrategy = getAuditStrategy();
        final Class<?> expectedClass = ValidityAuditStrategy.class;
        final Class<?> actualClass = auditStrategy.getClass();
        if (expectedClass.equals(actualClass)) {
            return "OK";
        }

        return "Expected that audit strategy " + actualClass.getName() + " is not as expected: " + expectedClass.getName();
    }
}
