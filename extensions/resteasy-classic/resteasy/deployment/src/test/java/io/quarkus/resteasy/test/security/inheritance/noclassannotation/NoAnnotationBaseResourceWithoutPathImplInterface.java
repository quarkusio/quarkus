package io.quarkus.resteasy.test.security.inheritance.noclassannotation;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_DENY_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public class NoAnnotationBaseResourceWithoutPathImplInterface extends NoAnnotationParentResourceWithoutPathImplInterface {

    @Override
    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH)
    public String classPathOnInterface_ImplOnBase_ImplMethodWithPath(JsonObject array) {
        throw new IllegalStateException("RESTEasy didn't support this endpoint in past");
    }

    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_NoAnnotation(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_NoSecurityAnnotation() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_BASE + NO_SECURITY_ANNOTATION_PATH);
    }

    @PermitAll
    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_MethodPermitAll() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_BASE + METHOD_PERMIT_ALL_PATH);
    }

    @DenyAll
    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_MethodDenyAll() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_BASE + METHOD_DENY_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Override
    public NoAnnotationSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_MethodRolesAllowed() {
        return new NoAnnotationSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_BASE + METHOD_ROLES_ALLOWED_PATH);
    }

    @Override
    public String classPathOnInterface_ImplOnBase_ParentMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        throw new IllegalStateException("RESTEasy didn't support this endpoint in past");
    }

}
