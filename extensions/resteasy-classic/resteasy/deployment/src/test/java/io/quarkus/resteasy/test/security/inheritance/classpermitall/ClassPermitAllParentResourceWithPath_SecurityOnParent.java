package io.quarkus.resteasy.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PREFIX;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_PARENT;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.json.JsonObject;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@PermitAll
@Path(CLASS_PERMIT_ALL_PREFIX + CLASS_SECURITY_ON_PARENT + CLASS_PATH_ON_PARENT_RESOURCE)
public abstract class ClassPermitAllParentResourceWithPath_SecurityOnParent
        implements ClassPermitAllInterfaceWithoutPath_PathOnParent_SecurityOnParent {

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + CLASS_PERMIT_ALL_PATH)
    public ClassPermitAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnParent_ClassPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT + CLASS_PERMIT_ALL_PATH);
    }

    @PermitAll
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_PARENT
            + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    public ClassPermitAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnParent_ClassPermitAllMethodPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT
                + SUB_IMPL_ON_PARENT + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH);
    }

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH)
    public String classPathOnParentResource_ImplOnParent_ImplMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    public String classPathOnParentResource_ImplOnParent_ImplMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnParentResource_ImplOnParent_InterfaceMethodWithPath_ClassPermitAllMethodPermitAll(
            JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH
                + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }
}
