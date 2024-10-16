package io.quarkus.resteasy.reactive.server.test.security.inheritance.noclassannotation;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public interface NoAnnotationParentResourceInterface {

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_NoSecurityAnnotation(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_MethodRolesAllowed(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_MethodDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_MethodPermitAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_NoSecurityAnnotation(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_MethodPermitAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_MethodDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_MethodRolesAllowed(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    default String classPathOnParentResource_ImplOnInterface_ImplMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    default String classPathOnParentResource_ImplOnInterface_ImplMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @PermitAll
    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    default String classPathOnParentResource_ImplOnInterface_ImplMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @DenyAll
    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    default String classPathOnParentResource_ImplOnInterface_ImplMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }
}
