
package io.quarkus.hibernate.orm.envers.config;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-allow-identifier-reuse")
@ApplicationScoped
public class EnversTestAllowIdentifierReuseResource extends AbstractEnversResource {
    @GET
    public String getAllowIdentifierReuse() {
        boolean identifierReuse = getGlobalConfiguration().isAllowIdentifierReuse();
        if (!identifierReuse) {
            return "Expected allow_identifier_reuse to be true but was false";
        }
        return "OK";
    }
}
