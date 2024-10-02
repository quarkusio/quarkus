package io.quarkus.resteasy.test.security.inheritance.noclassannotation;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_DENY_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public abstract class NoAnnotationParentResourceWithoutPath implements NoAnnotationInterfaceWithoutPath {

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_NoSecurityAnnotation(JsonObject array);

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_RolesAllowed(JsonObject array);

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_DenyAll(JsonObject array);

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_PermitAll(JsonObject array);

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_NoSecurityAnnotation(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_NoAnnotation(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_MethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH;
    }

    @DenyAll
    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_MethodDenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_MethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + METHOD_PERMIT_ALL_PATH;
    }

    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_MethodRolesAllowed(JsonObject array) {
        // hint: purpose of this method is to ensure that existence of overridden parent method
        //   has no effect on a secured method (like: correct secured resource method is identified)
        throw new IllegalStateException("Implementation should be used");
    }
}
