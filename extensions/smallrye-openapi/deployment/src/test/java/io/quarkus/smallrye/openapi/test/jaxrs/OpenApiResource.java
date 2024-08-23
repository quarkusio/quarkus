package io.quarkus.smallrye.openapi.test.jaxrs;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/resource")
@Tag(name = "test")
@Extension(name = "openApiExtension", value = "openApiExtensionValue")
@SecurityRequirement(name = "securityRequirement", scopes = "securityRequirementScope")
@Server(url = "serverUrl")
public class OpenApiResource {

    private ResourceBean resourceBean;

    @Inject
    public OpenApiResource(ResourceBean resourceBean) {
        this.resourceBean = resourceBean;
    }

    @GET
    public String root() {
        return resourceBean.toString();
    }

    @GET
    @Path("/test-enums")
    public Query testEnums(@QueryParam("query") Query query) {
        return query;
    }

    public enum Query {
        QUERY_PARAM_1,
        QUERY_PARAM_2;
    }
}
