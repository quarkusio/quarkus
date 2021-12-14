
package io.quarkus.hibernate.orm.envers.config;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-use-revision-entity-with-native-id")
@ApplicationScoped
public class EnversTestUseRevisionEntityWithNativeIdResource extends AbstractEnversResource {
    @GET
    public String getUseRevisionEntityWithNativeId() {
        boolean revisionEntityWithNativeId = getGlobalConfiguration().isUseRevisionEntityWithNativeId();
        if (revisionEntityWithNativeId) {
            return "Expected use_revision_entity_with_native_id to be false but was true";
        }
        return "OK";
    }
}
