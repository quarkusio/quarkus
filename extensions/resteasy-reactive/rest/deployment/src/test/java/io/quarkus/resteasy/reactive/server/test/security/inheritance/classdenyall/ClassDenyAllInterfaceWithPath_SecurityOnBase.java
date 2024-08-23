package io.quarkus.resteasy.reactive.server.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PREFIX;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.json.JsonObject;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path(CLASS_DENY_ALL_PREFIX + CLASS_SECURITY_ON_BASE + CLASS_PATH_ON_INTERFACE)
public interface ClassDenyAllInterfaceWithPath_SecurityOnBase {

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    String classPathOnInterface_ImplOnBase_ImplMethodWithPath_ClassDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    String classPathOnInterface_ImplOnBase_ImplMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    String classPathOnInterface_ImplOnBase_ImplMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_ClassDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodRolesAllowed(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_ClassDenyAllMethodPermitAll(JsonObject array);

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_PATH)
    ClassDenyAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_ClassDenyAll();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    ClassDenyAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_ClassDenyAllMethodPermitAll();

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    ClassDenyAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_ClassDenyAllMethodRolesAllowed();

}
