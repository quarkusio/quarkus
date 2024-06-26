package io.quarkus.it.opentelemetry.reactive;

import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import io.opentelemetry.api.trace.Tracer;

@Path("/otel/enduser")
public class EndUserResource {

    @Inject
    Tracer tracer;

    @Path("/no-authorization")
    @GET
    public String noAuthorization() {
        return "/no-authorization";
    }

    @RolesAllowed("WRITER")
    @Path("/roles-allowed-only-writer-role")
    @GET
    public String rolesAllowedOnlyWriterRole() {
        return "/roles-allowed-only-writer-role";
    }

    @PermitAll
    @Path("/permit-all-only")
    @GET
    public String permitAllOnly() {
        return "/permit-all-only";
    }

    @Path("/no-authorization-augmentor")
    @GET
    public String noAuthorizationAugmentor() {
        return "/no-authorization-augmentor";
    }

    @RolesAllowed("AUGMENTOR")
    @Path("/roles-allowed-only-augmentor-role")
    @GET
    public String rolesAllowedOnlyAugmentorRole() {
        return "/roles-allowed-only-augmentor-role";
    }

    @PermitAll
    @Path("/permit-all-only-augmentor")
    @GET
    public String permitAllOnlyAugmentor() {
        return "/permit-all-only-augmentor";
    }

    @RolesAllowed("WRITER-HTTP-PERM")
    @Path("/roles-allowed-writer-http-perm-role")
    @GET
    public String rolesAllowedHttpPermWriterHttpPermRole() {
        return "/roles-allowed-writer-http-perm-role";
    }

    @PermitAll
    @Path("/roles-mapping-http-perm")
    @GET
    public String permitAllAnnotationConfigRolesMappingPermitAllHttpPerm() {
        return "/roles-mapping-http-perm";
    }

    @RolesAllowed("HTTP-PERM-AUGMENTOR")
    @Path("/roles-allowed-http-perm-augmentor-role")
    @GET
    public String rolesAllowedHttpPermHttpAugmentorPermRole() {
        return "/roles-allowed-http-perm-augmentor-role";
    }

    @PermitAll
    @Path("/roles-mapping-http-perm-augmentor")
    @GET
    public String permitAllAnnotationConfigRolesMappingPermitAllHttpPermAugmentor() {
        return "/roles-mapping-http-perm-augmentor";
    }

    @Path("/jax-rs-http-perm")
    @GET
    public String jaxRsHttpPermOnly() {
        return "/jax-rs-http-perm";
    }

    @RolesAllowed("READER")
    @Path("/jax-rs-http-perm-annotation-reader-role")
    @GET
    public String jaxRsHttpPermRolesAllowedReaderRole() {
        return "/jax-rs-http-perm-annotation-reader-role";
    }

    @RolesAllowed("READER")
    @Path("/custom-span-reader-role")
    @GET
    public String customSpanReaderRole() {
        var span = tracer.spanBuilder("custom-span").startSpan();
        try (var ignored = span.makeCurrent()) {
            span.setAttribute("custom_attribute", "custom-value");
            span.setAttribute(URL_PATH, "custom-path");
        } finally {
            span.end();
        }
        return "/custom-span-reader-role";
    }
}
