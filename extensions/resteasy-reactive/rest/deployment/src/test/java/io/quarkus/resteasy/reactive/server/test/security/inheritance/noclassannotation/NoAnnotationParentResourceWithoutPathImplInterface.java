package io.quarkus.resteasy.reactive.server.test.security.inheritance.noclassannotation;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_PARENT;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public abstract class NoAnnotationParentResourceWithoutPathImplInterface implements NoAnnotationInterfaceWithPath {

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    public abstract String classPathOnInterface_ImplOnBase_ParentMethodWithPath_NoSecurityAnnotation(JsonObject array);

    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_NoSecurityAnnotation() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_PARENT + NO_SECURITY_ANNOTATION_PATH);
    }

    @PermitAll
    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_MethodPermitAll() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_PARENT + METHOD_PERMIT_ALL_PATH);
    }

    @DenyAll
    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_MethodDenyAll() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_PARENT + METHOD_DENY_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_MethodRolesAllowed() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_PARENT + METHOD_ROLES_ALLOWED_PATH);
    }

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    public String classPathOnInterface_ImplOnParent_ImplMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @Override
    public String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @DenyAll
    @Override
    public String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }
}
