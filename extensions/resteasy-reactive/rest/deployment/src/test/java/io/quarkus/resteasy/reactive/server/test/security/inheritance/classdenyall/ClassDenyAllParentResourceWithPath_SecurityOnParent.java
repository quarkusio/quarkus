package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PREFIX;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_PARENT;
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
@DenyAll
@Path(CLASS_DENY_ALL_PREFIX + CLASS_SECURITY_ON_PARENT + CLASS_PATH_ON_PARENT_RESOURCE)
public abstract class ClassDenyAllParentResourceWithPath_SecurityOnParent
        implements ClassDenyAllInterfaceWithoutPath_PathOnParent_SecurityOnParent {

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + CLASS_DENY_ALL_PATH)
    public ClassDenyAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnParent_ClassDenyAll() {
        return new ClassDenyAllSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + CLASS_DENY_ALL_PATH);
    }

    @PermitAll
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT
            + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    public ClassDenyAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnParent_ClassDenyAllMethodPermitAll() {
        return new ClassDenyAllSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT
                        + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH);
    }

    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT
            + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    public ClassDenyAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnParent_ClassDenyAllMethodRolesAllowed() {
        return new ClassDenyAllSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT
                        + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH);
    }

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    public String classPathOnParentResource_ImplOnParent_ImplMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @RolesAllowed("admin")
    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    public String classPathOnParentResource_ImplOnParent_ImplMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH
                + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

    @PermitAll
    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    public String classPathOnParentResource_ImplOnParent_ImplMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH
                + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_ClassDenyAllMethodRolesAllowed(
            JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH
                + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }
}
