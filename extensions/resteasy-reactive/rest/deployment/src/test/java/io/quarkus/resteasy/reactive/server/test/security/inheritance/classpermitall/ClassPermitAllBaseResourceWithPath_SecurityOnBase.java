package io.quarkus.resteasy.reactive.server.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PREFIX;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

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
@Path(CLASS_PERMIT_ALL_PREFIX + CLASS_SECURITY_ON_BASE + CLASS_PATH_ON_RESOURCE)
public class ClassPermitAllBaseResourceWithPath_SecurityOnBase extends ClassPermitAllParentResourceWithoutPath_SecurityOnBase {

    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH)
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_ClassPermitAllPath(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_ClassPermitAllMethodPermitAllPath(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_PERMIT_ALL_PATH)
    public ClassPermitAllSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_ClassPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_PERMIT_ALL_PATH);
    }

    @PermitAll
    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    public ClassPermitAllSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_ClassPermitAllMethodPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH);
    }
}
