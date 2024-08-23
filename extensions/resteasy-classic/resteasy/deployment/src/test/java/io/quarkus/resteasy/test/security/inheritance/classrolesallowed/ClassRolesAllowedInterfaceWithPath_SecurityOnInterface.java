package io.quarkus.resteasy.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PREFIX;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_INTERFACE;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.json.JsonObject;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
@Path(CLASS_ROLES_ALLOWED_PREFIX + CLASS_SECURITY_ON_INTERFACE + CLASS_PATH_ON_INTERFACE)
public interface ClassRolesAllowedInterfaceWithPath_SecurityOnInterface {

    @Path(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE + SUB_IMPL_ON_INTERFACE + CLASS_ROLES_ALLOWED_PATH)
    default ClassRolesAllowedSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnInterface_ClassRolesAllowed() {
        return new ClassRolesAllowedSubResourceWithoutPath(
                CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                        + SUB_IMPL_ON_INTERFACE + CLASS_ROLES_ALLOWED_PATH);
    }

    @POST
    @Path(CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH)
    default String classPathOnInterface_ImplOnInterface_ImplMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_INTERFACE + IMPL_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

}
