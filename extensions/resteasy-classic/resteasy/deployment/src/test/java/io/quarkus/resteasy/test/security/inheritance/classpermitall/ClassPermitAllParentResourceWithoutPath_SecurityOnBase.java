package io.quarkus.resteasy.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public abstract class ClassPermitAllParentResourceWithoutPath_SecurityOnBase
        implements ClassPermitAllInterfaceWithoutPath_SecurityOnBase {

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassPermitAll(JsonObject array);

    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    @POST
    public abstract String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassPermitAllMethodPermitAll(
            JsonObject array);

}
