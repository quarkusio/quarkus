package io.quarkus.resteasy.test.security.inheritance.noclassannotation;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_NO_ANNOTATION_PREFIX;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_DENY_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_PARENT;

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
@Path(CLASS_NO_ANNOTATION_PREFIX + CLASS_PATH_ON_PARENT_RESOURCE)
public abstract class NoAnnotationParentResourceWithPath implements NoAnnotationParentResourceInterface {

    public String get_ClassPathOnParentResource_ImplOnBase_ImplMethodWithPath_NoAnnotation(JsonObject array) {
        throw new IllegalStateException("Implementation should had been invoked");
    }

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    public abstract String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_NoAnnotation(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    public abstract String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_MethodRolesAllowed(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    public abstract String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_MethodDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    public abstract String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_MethodPermitAll(JsonObject array);

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + NO_SECURITY_ANNOTATION_PATH)
    public NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnParentResource_NoSecurityAnnotation() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT
                + SUB_IMPL_ON_PARENT + NO_SECURITY_ANNOTATION_PATH);
    }

    @PermitAll
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + METHOD_PERMIT_ALL_PATH)
    public NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnParentResource_MethodPermitAll() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + METHOD_PERMIT_ALL_PATH);
    }

    @DenyAll
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + METHOD_DENY_ALL_PATH)
    public NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnParentResource_MethodDenyAll() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + METHOD_DENY_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + METHOD_ROLES_ALLOWED_PATH)
    public NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnParentResource_MethodRolesAllowed() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT
                + SUB_IMPL_ON_PARENT + METHOD_ROLES_ALLOWED_PATH);
    }

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + NO_SECURITY_ANNOTATION_PATH)
    public abstract NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBaseResource_NoSecurityAnnotation();

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + METHOD_PERMIT_ALL_PATH)
    public abstract NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBaseResource_MethodPermitAll();

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + METHOD_DENY_ALL_PATH)
    public abstract NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBaseResource_MethodDenyAll();

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + METHOD_ROLES_ALLOWED_PATH)
    public abstract NoAnnotationSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBaseResource_MethodRolesAllowed();

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    public String classPathOnParentResource_ImplOnParent_ImplMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @POST
    @PermitAll
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    public String classPathOnParentResource_ImplOnParent_ImplMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @POST
    @DenyAll
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    public String classPathOnParentResource_ImplOnParent_ImplMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @POST
    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    public String classPathOnParentResource_ImplOnParent_ImplMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @Override
    public String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Override
    public String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }
}
