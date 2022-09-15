
package io.quarkus.hibernate.orm.envers.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-revision-on-collection-change")
@ApplicationScoped
public class EnversTestRevisionOnCollectionChangeResource extends AbstractEnversResource {
    @GET
    public String getRevisionOnCollectionChange() {
        boolean revisionsForCollections = getGlobalConfiguration().isGenerateRevisionsForCollections();
        if (revisionsForCollections) {
            return "Expected revision_on_collect_change to be false but was true";
        }
        return "OK";
    }
}
