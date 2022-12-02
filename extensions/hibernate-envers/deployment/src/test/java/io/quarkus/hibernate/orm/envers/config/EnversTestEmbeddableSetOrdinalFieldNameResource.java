
package io.quarkus.hibernate.orm.envers.config;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-embeddable-set-ordinal-field-name")
@ApplicationScoped
public class EnversTestEmbeddableSetOrdinalFieldNameResource extends AbstractEnversResource {
    @GET
    public String getEntityWithEmbeddableSetMappingNameOverride() {
        String embeddableSetOrdinalName = getAuditEntitiesConfiguration().getEmbeddableSetOrdinalPropertyName();
        if (embeddableSetOrdinalName.equals("ORD")) {
            return "OK";
        }
        return "Expected ORD as embeddable_set_ordinal_field_name but was: " + embeddableSetOrdinalName;
    }
}
