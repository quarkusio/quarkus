package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PREFIX;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_INTERFACE;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.json.JsonObject;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@DenyAll
@Path(CLASS_DENY_ALL_PREFIX + CLASS_SECURITY_ON_INTERFACE + CLASS_PATH_ON_INTERFACE)
public interface ClassDenyAllInterfaceWithPath_SecurityOnInterface {

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + CLASS_DENY_ALL_PATH)
    default ClassDenyAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_ClassDenyAll() {
        return new ClassDenyAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_INTERFACE + CLASS_DENY_ALL_PATH);
    }

    @PermitAll
    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    default ClassDenyAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_ClassDenyAllMethodPermitAll() {
        return new ClassDenyAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_INTERFACE + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE
            + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    default ClassDenyAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_ClassDenyAllMethodRolesAllowed() {
        return new ClassDenyAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_INTERFACE + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH);
    }

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @PermitAll
    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @RolesAllowed("admin")
    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

}
