package io.quarkus.resteasy.reactive.server.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.annotation.security.RolesAllowed;

import io.vertx.core.json.JsonObject;

@RolesAllowed("admin")
public class ClassRolesAllowedBaseResourceWithoutPathImplInterface_SecurityOnBase
        implements ClassRolesAllowedInterfaceWithPath_SecurityOnBase {

    @Override
    public String classPathOnInterface_ImplOnBase_ImplMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

    @Override
    public ClassRolesAllowedSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_ClassRolesAllowed() {
        return new ClassRolesAllowedSubResourceWithoutPath(
                CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                        + SUB_IMPL_ON_BASE + CLASS_ROLES_ALLOWED_PATH);
    }
}
