package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public abstract class ClassDenyAllParentResourceWithoutPath_SecurityOnBase
        implements ClassDenyAllInterfaceWithoutPath_SecurityOnBase {

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassDenyAll(JsonObject array);

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassDenyAllMethodPermitAll(
            JsonObject array);

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassDenyAllMethodRolesAllowed(
            JsonObject array);

}
