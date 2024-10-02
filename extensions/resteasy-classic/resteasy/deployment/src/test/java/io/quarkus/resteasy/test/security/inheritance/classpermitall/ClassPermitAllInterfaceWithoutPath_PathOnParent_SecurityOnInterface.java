package io.quarkus.resteasy.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

@PermitAll
public interface ClassPermitAllInterfaceWithoutPath_PathOnParent_SecurityOnInterface {

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH)
    default String classPathOnParentResource_ImplOnInterface_ImplMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    default String classPathOnParentResource_ImplOnInterface_ImplMethodWithPath_ClassPermitAllMethodPermitAll(
            JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH
                + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

}
