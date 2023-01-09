
package io.quarkus.hibernate.orm.envers.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.hibernate.persister.entity.EntityPersister;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;
import io.quarkus.hibernate.orm.envers.MyAuditedEntity;

@Path("/envers-original-id-prop-name")
@ApplicationScoped
public class EnversTestOriginalIdPropNameResource extends AbstractEnversResource {
    @GET
    public String getOriginalIdPropNameOverride() {
        String originalIdFieldName = getAuditEntitiesConfiguration().getOriginalIdPropName();
        if (!originalIdFieldName.equals("oid")) {
            return "Expected original_id_prop_name to be oid but was: " + originalIdFieldName;
        }

        EntityPersister persister = getEntityPersister(getDefaultAuditEntityName(MyAuditedEntity.class));
        if (!persister.getIdentifierPropertyName().equals("oid")) {
            return "Expected identifier property name to be oid but was: " + persister.getIdentifierPropertyName();
        }

        return "OK";
    }
}
