package io.quarkus.resteasy.test.security.inheritance.noclassannotation;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_NO_ANNOTATION_PREFIX;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_DENY_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public class NoAnnotationBaseResourceWithoutPathExtParentRes extends NoAnnotationParentResourceWithPath {

    @Override
    @Path(CLASS_NO_ANNOTATION_PREFIX + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    @POST
    public String get_ClassPathOnParentResource_ImplOnBase_ImplMethodWithPath_NoAnnotation(JsonObject array) {
        return CLASS_NO_ANNOTATION_PREFIX + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_NoAnnotation(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBaseResource_NoSecurityAnnotation() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + NO_SECURITY_ANNOTATION_PATH);
    }

    @PermitAll
    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBaseResource_MethodPermitAll() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + METHOD_PERMIT_ALL_PATH);
    }

    @DenyAll
    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBaseResource_MethodDenyAll() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + METHOD_DENY_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBaseResource_MethodRolesAllowed() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + METHOD_ROLES_ALLOWED_PATH);
    }

    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }
}
