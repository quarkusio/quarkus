package io.quarkus.resteasy.reactive.server.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public abstract class ClassRolesAllowedParentResourceWithoutPath_SecurityOnBase
        implements ClassRolesAllowedInterfaceWithoutPath_SecurityOnBase {

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassRolesAllowed(JsonObject array);

}
