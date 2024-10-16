package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public interface ClassDenyAllParentResourceInterface_SecurityOnBase {

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_ClassDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array);

}
