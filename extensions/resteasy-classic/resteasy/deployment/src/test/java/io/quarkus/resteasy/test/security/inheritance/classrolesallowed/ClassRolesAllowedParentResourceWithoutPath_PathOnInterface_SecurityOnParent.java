package io.quarkus.resteasy.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_PARENT;

import jakarta.annotation.security.RolesAllowed;

import io.vertx.core.json.JsonObject;

@RolesAllowed("admin")
public abstract class ClassRolesAllowedParentResourceWithoutPath_PathOnInterface_SecurityOnParent
        implements ClassRolesAllowedInterfaceWithPath_SecurityOnParent {

    @Override
    public ClassRolesAllowedSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_ClassRolesAllowed() {
        return new ClassRolesAllowedSubResourceWithoutPath(
                CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                        + SUB_IMPL_ON_PARENT + CLASS_ROLES_ALLOWED_PATH);
    }

    @Override
    public String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }
}
