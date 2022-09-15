
package io.quarkus.hibernate.orm.envers.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.quarkus.hibernate.orm.envers.AbstractEnversResource;

@Path("/envers-default-schema-catalog")
@ApplicationScoped
public class EnversTestDefaultSchemaCatalogResource extends AbstractEnversResource {
    @GET
    public String getDefaultSchemaAndCatalog() {
        String defaultSchema = getGlobalConfiguration().getDefaultSchemaName();
        if (!"public".equals(defaultSchema)) {
            return "Expected default_schema to be public but was: " + defaultSchema;
        }

        String defaultCatalog = getGlobalConfiguration().getDefaultCatalogName();
        if (!"".equals(defaultCatalog)) {
            return "Expected default_catalog to be an empty string but was: " + defaultCatalog;
        }

        return "OK";
    }
}
