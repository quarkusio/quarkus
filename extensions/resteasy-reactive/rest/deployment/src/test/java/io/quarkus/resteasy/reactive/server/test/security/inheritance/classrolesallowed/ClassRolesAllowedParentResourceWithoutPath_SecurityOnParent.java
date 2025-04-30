package io.quarkus.resteasy.reactive.server.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;

import jakarta.annotation.security.RolesAllowed;

import io.vertx.core.json.JsonObject;

@RolesAllowed("admin")
public abstract class ClassRolesAllowedParentResourceWithoutPath_SecurityOnParent
        implements ClassRolesAllowedInterfaceWithoutPath_SecurityOnParent {

    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }
}
