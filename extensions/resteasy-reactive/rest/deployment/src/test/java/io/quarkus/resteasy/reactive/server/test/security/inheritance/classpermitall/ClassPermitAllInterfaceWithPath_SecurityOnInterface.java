package io.quarkus.resteasy.reactive.server.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PREFIX;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_INTERFACE;

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
@Path(CLASS_PERMIT_ALL_PREFIX + CLASS_SECURITY_ON_INTERFACE + CLASS_PATH_ON_INTERFACE)
public interface ClassPermitAllInterfaceWithPath_SecurityOnInterface {

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + CLASS_PERMIT_ALL_PATH)
    default ClassPermitAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_ClassPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_INTERFACE + CLASS_PERMIT_ALL_PATH);
    }

    @PermitAll
    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    default ClassPermitAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_ClassPermitAllMethodPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_INTERFACE + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH);
    }

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

}
