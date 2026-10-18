package io.quarkus.resteasy.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public interface ClassDenyAllInterfaceWithoutPath_SecurityOnParent {

    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array);

}
