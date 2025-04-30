package io.quarkus.resteasy.reactive.server.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

@RolesAllowed("admin")
public abstract class ClassRolesAllowedParentResourceWithoutPath_PathOnBase_SecurityOnParent {

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

}
