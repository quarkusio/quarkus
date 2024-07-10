package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

@DenyAll
public abstract class ClassDenyAllParentResourceWithoutPath_PathOnBase_SecurityOnParent
        implements ClassDenyAllInterfaceWithoutPath_SecurityOnParent {

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @PermitAll
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @RolesAllowed("admin")
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }

    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassDenyAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
    }
}
