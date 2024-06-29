package io.quarkus.resteasy.test.security.inheritance.classrolesallowed;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_ROLES_ALLOWED_PREFIX;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

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
@Path(CLASS_ROLES_ALLOWED_PREFIX + CLASS_SECURITY_ON_BASE + CLASS_PATH_ON_RESOURCE)
public class ClassRolesAllowedBaseResourceWithPath_SecurityOnBase
        extends ClassRolesAllowedParentResourceWithoutPath_SecurityOnBase {

    @POST
    @Path(CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH)
    public String get_ClassPathOnResource_ImplOnBase_ImplMethodWithPath_ClassRolesAllowedPath(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

    @Override
    public String classPathOnResource_ImplOnBase_InterfaceMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

    @Override
    public String test_ClassPathOnResource_ImplOnBase_ParentMethodWithPath_ClassRolesAllowed(JsonObject array) {
        return CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_ROLES_ALLOWED_PATH;
    }

    @Path(CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_ROLES_ALLOWED_PATH)
    public ClassRolesAllowedSubResourceWithoutPath classPathOnResource_SubDeclaredOnBase_SubImplOnBase_ClassRolesAllowed() {
        return new ClassRolesAllowedSubResourceWithoutPath(
                CLASS_PATH_ON_RESOURCE + SUB_DECLARED_ON_BASE + SUB_IMPL_ON_BASE + CLASS_ROLES_ALLOWED_PATH);
    }
}
