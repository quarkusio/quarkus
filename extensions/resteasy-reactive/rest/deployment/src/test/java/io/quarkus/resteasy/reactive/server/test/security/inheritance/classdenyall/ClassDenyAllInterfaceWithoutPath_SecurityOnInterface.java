package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

@DenyAll
public interface ClassDenyAllInterfaceWithoutPath_SecurityOnInterface {

    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    default String classPathOnResource_ImplOnInterface_ImplMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @PermitAll
    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    default String classPathOnResource_ImplOnInterface_ImplMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @RolesAllowed("admin")
    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    default String classPathOnResource_ImplOnInterface_ImplMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

}
