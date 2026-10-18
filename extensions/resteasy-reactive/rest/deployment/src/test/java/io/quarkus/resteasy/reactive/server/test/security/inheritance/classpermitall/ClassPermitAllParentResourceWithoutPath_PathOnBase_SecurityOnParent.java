package io.quarkus.resteasy.reactive.server.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

@PermitAll
public abstract class ClassPermitAllParentResourceWithoutPath_PathOnBase_SecurityOnParent
        implements ClassPermitAllInterfaceWithoutPath_SecurityOnParent {

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    @POST
    public String test_ClassPathOnResource_ImplOnParent_ImplMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnResource_ImplOnParent_InterfaceMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }
}
