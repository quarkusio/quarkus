package io.quarkus.resteasy.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public interface ClassRolesAllowedInterfaceWithoutPath_SecurityOnParent {

    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH)
    String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassRolesAllowed(JsonObject array);

}
