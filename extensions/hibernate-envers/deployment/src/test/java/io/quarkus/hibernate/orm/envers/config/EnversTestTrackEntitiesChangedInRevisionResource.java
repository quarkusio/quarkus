
package io.quarkus.hibernate.orm.envers.config;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-track-entities-changed-in-revision")
@ApplicationScoped
public class EnversTestTrackEntitiesChangedInRevisionResource extends AbstractEnversResource {
    @GET
    public String getTrackEntitiesChangedInRevision() {
        boolean trackEntityChangesInRevision = getGlobalConfiguration().isTrackEntitiesChangedInRevision();
        if (!trackEntityChangesInRevision) {
            return "Expected track_entities_changed_in_revision to be true but was false";
        }
        return "OK";
    }
}
