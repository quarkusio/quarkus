package io.quarkus.resteasy.reactive.server.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PREFIX;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_PARENT;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.json.JsonObject;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path(CLASS_PERMIT_ALL_PREFIX + CLASS_SECURITY_ON_PARENT + CLASS_PATH_ON_INTERFACE)
public interface ClassPermitAllInterfaceWithPath_SecurityOnParent {

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_PARENT + CLASS_PERMIT_ALL_PATH)
    ClassPermitAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_ClassPermitAll();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_PARENT
            + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    ClassPermitAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_ClassPermitAllMethodPermitAll();

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH)
    String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_ClassPermitAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array);

}
