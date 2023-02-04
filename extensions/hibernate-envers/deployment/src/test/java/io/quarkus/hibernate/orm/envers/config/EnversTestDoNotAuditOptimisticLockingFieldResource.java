
package io.quarkus.hibernate.orm.envers.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.persister.entity.EntityPersister;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedVersionEntity;

@Path("/envers-do-not-audit-optimistic-locking-field")
@ApplicationScoped
public class EnversTestDoNotAuditOptimisticLockingFieldResource extends AbstractEnversResource {
    @GET
    public String getDoNotAuditOptimisticLockingFieldDisabled() {
        if (!getGlobalConfiguration().isDoNotAuditOptimisticLockingField()) {
            EntityPersister persister = getEntityPersister(MyAuditedVersionEntity.class.getName() + "_AUD");
            for (String propertyName : persister.getPropertyNames()) {
                if (propertyName.equals("version")) {
                    return "OK";
                }
            }
        }
        return "Expected false is not as expected: " + getGlobalConfiguration().isDoNotAuditOptimisticLockingField();
    }
}
