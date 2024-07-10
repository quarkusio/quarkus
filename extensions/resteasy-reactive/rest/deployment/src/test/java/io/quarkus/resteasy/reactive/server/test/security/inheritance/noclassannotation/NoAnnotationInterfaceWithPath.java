package io.quarkus.resteasy.reactive.server.test.security.inheritance.noclassannotation;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_NO_ANNOTATION_PREFIX;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_PARENT;

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
@Path(CLASS_NO_ANNOTATION_PREFIX + CLASS_PATH_ON_INTERFACE)
public interface NoAnnotationInterfaceWithPath {

    String classPathOnInterface_ImplOnBase_ImplMethodWithPath_NoAnnotation(JsonObject array);

    String classPathOnInterface_ImplOnBase_ImplMethodWithPath_MethodRolesAllowed(JsonObject array);

    String classPathOnInterface_ImplOnBase_ImplMethodWithPath_MethodDenyAll(JsonObject array);

    String classPathOnInterface_ImplOnBase_ImplMethodWithPath_MethodPermitAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_NoAnnotation(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_MethodRolesAllowed(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_MethodDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_MethodPermitAll(JsonObject array);

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + NO_SECURITY_ANNOTATION_PATH)
    default NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_NoSecurityAnnotation() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_INTERFACE + NO_SECURITY_ANNOTATION_PATH);
    }

    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + METHOD_ROLES_ALLOWED_PATH)
    default NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_MethodRolesAllowed() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_INTERFACE + METHOD_ROLES_ALLOWED_PATH);
    }

    @DenyAll
    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + METHOD_DENY_ALL_PATH)
    default NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_MethodDenyAll() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + METHOD_DENY_ALL_PATH);
    }

    @PermitAll
    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + METHOD_PERMIT_ALL_PATH)
    default NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_MethodPermitAll() {
        return new NoAnnotationSubResourceWithoutPath(
                CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + METHOD_PERMIT_ALL_PATH);
    }

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE + NO_SECURITY_ANNOTATION_PATH)
    NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_NoSecurityAnnotation();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE + METHOD_PERMIT_ALL_PATH)
    NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_MethodPermitAll();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE + METHOD_DENY_ALL_PATH)
    NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_MethodDenyAll();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE + METHOD_ROLES_ALLOWED_PATH)
    NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_MethodRolesAllowed();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_PARENT + NO_SECURITY_ANNOTATION_PATH)
    NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_NoSecurityAnnotation();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_PARENT + METHOD_PERMIT_ALL_PATH)
    NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_MethodPermitAll();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_PARENT + METHOD_DENY_ALL_PATH)
    NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_MethodDenyAll();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_PARENT + METHOD_ROLES_ALLOWED_PATH)
    NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_MethodRolesAllowed();

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_NoSecurityAnnotation(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_MethodDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_MethodPermitAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_MethodRolesAllowed(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @PermitAll
    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @DenyAll
    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }
}
