package io.quarkus.resteasy.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import io.vertx.core.json.JsonObject;

@DenyAll
public class ClassDenyAllBaseResourceWithoutPathImplInterface_SecurityOnBase
        implements ClassDenyAllInterfaceWithPath_SecurityOnBase {

    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public ClassDenyAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_ClassDenyAll() {
        return new ClassDenyAllSubResourceWithoutPath(
                CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_PATH);
    }

    @PermitAll
    @Override
    public ClassDenyAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_ClassDenyAllMethodPermitAll() {
        return new ClassDenyAllSubResourceWithoutPath(
                CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Override
    public ClassDenyAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_ClassDenyAllMethodRolesAllowed() {
        return new ClassDenyAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH);
    }
}
