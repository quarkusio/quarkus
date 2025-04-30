package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import io.vertx.core.json.JsonObject;

@DenyAll
public class ClassDenyAllBaseResourceWithoutPathExtParentRes_SecurityOnBase
        extends ClassDenyAllParentResourceWithPath_SecurityOnBase {

    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodRolesAllowed(
            JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH
                + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH
                + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH
                + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Override
    public ClassDenyAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBase_ClassDenyAll() {
        return new ClassDenyAllSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_PATH);
    }

    @PermitAll
    @Override
    public ClassDenyAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBase_ClassDenyAllMethodPermitAll() {
        return new ClassDenyAllSubResourceWithoutPath(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE
                + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Override
    public ClassDenyAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBase_ClassDenyAllMethodRolesAllowed() {
        return new ClassDenyAllSubResourceWithoutPath(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE
                + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH);
    }
}
