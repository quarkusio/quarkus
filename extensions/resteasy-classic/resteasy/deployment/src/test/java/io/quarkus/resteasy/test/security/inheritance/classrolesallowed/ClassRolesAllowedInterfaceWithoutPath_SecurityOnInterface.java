package io.quarkus.resteasy.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

@RolesAllowed("admin")
public interface ClassRolesAllowedInterfaceWithoutPath_SecurityOnInterface {

    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH)
    default String classPathOnResource_ImplOnInterface_ImplMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

}
