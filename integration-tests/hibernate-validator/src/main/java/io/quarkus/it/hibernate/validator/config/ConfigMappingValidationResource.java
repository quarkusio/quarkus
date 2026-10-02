package io.quarkus.it.hibernate.validator.config;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Exposes the config mappings from this package so a test can assert that the application actually
 * started and resolved each one correctly. In native mode, simply reaching any of these endpoints proves
 * the config mapping validator didn't blow up during startup for any of the three shapes.
 */
@Path("/config-mapping-validation")
public class ConfigMappingValidationResource {

    @Inject
    NotValidatedConfig notValidatedConfig;

    @Inject
    RootValidatedConfig rootValidatedConfig;

    @Inject
    NestedValidatedConfig nestedValidatedConfig;

    @GET
    @Path("/not-validated")
    @Produces(MediaType.TEXT_PLAIN)
    public String notValidated() {
        return notValidatedConfig.value();
    }

    @GET
    @Path("/root-validated")
    @Produces(MediaType.TEXT_PLAIN)
    public String rootValidated() {
        return rootValidatedConfig.value();
    }

    @GET
    @Path("/nested-validated")
    @Produces(MediaType.TEXT_PLAIN)
    public String nestedValidated() {
        return nestedValidatedConfig.nested().deeper().value();
    }
}
