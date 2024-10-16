package io.quarkus.resteasy.test.security.inheritance.noclassannotation;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_NO_ANNOTATION_PREFIX;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_DENY_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.json.JsonObject;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path(CLASS_NO_ANNOTATION_PREFIX + CLASS_PATH_ON_RESOURCE)
public class NoAnnotationBaseResourceWithPath extends NoAnnotationParentResourceWithoutPath {

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    @POST
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_NoAnnotation(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @Override
    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    @POST
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    @POST
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    @POST
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_RolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_DenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_PermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_RolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_DenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_PermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + NO_SECURITY_ANNOTATION_PATH)
    public NoAnnotationSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_NoSecurityAnnotation() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + NO_SECURITY_ANNOTATION_PATH);
    }

    @PermitAll
    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + METHOD_PERMIT_ALL_PATH)
    public NoAnnotationSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_MethodPermitAll() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + METHOD_PERMIT_ALL_PATH);
    }

    @DenyAll
    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + METHOD_DENY_ALL_PATH)
    public NoAnnotationSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_MethodDenyAll() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + METHOD_DENY_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + METHOD_ROLES_ALLOWED_PATH)
    public NoAnnotationSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_MethodRolesAllowed() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + METHOD_ROLES_ALLOWED_PATH);
    }
}
