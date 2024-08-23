package io.quarkus.resteasy.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.annotation.security.RolesAllowed;

import io.vertx.core.json.JsonObject;

@RolesAllowed("admin")
public class ClassRolesAllowedBaseResourceWithoutPathExtParentRes_SecurityOnBase
        extends ClassRolesAllowedParentResourceWithPath_SecurityOnBase {

    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

    @Override
    public ClassRolesAllowedSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBaseResource_ClassRolesAllowed() {
        return new ClassRolesAllowedSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + CLASS_ROLES_ALLOWED_PATH);
    }
}
