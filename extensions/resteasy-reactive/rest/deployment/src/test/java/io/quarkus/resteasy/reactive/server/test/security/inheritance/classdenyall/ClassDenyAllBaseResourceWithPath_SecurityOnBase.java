package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PREFIX;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

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
@DenyAll
@Path(CLASS_DENY_ALL_PREFIX + CLASS_SECURITY_ON_BASE + CLASS_PATH_ON_RESOURCE)
public class ClassDenyAllBaseResourceWithPath_SecurityOnBase extends ClassDenyAllParentResourceWithoutPath_SecurityOnBase {

    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_ClassDenyAllPath(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @PermitAll
    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_ClassDenyAllMethodPermitAllPath(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @RolesAllowed("admin")
    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_ClassDenyAllMethodRolesAllowedPath(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_PATH)
    public ClassDenyAllSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_ClassDenyAll() {
        return new ClassDenyAllSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_PATH);
    }

    @PermitAll
    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    public ClassDenyAllSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_ClassDenyAllMethodPermitAll() {
        return new ClassDenyAllSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    public ClassDenyAllSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_ClassDenyAllMethodRolesAllowed() {
        return new ClassDenyAllSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH);
    }
}
