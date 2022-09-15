
package io.quarkus.hibernate.orm.envers.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-modified-flags")
@ApplicationScoped
public class EnversTestModifiedFlagsResource extends AbstractEnversResource {
    @GET
    public String getModifiedFlags() {
        boolean globalWithModifiedFlags = getGlobalConfiguration().isGlobalWithModifiedFlag();
        if (!globalWithModifiedFlags) {
            return "Expected global_with_modified_flag to be true but was false";
        }

        String modifiedFlagSuffix = getGlobalConfiguration().getModifiedFlagSuffix();
        if (!"_changed".equals(modifiedFlagSuffix)) {
            return "Expected modified_flag_suffix to be _changed but was: " + modifiedFlagSuffix;
        }

        return "OK";
    }
}
