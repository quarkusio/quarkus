package io.quarkus.resteasy.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PREFIX;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_DECLARED_ON_PARENT;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import io.vertx.core.json.JsonObject;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path(CLASS_DENY_ALL_PREFIX + CLASS_SECURITY_ON_BASE + CLASS_PATH_ON_PARENT_RESOURCE)
public abstract class ClassDenyAllParentResourceWithPath_SecurityOnBase
        implements ClassDenyAllParentResourceInterface_SecurityOnBase {

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_PATH)
    public abstract String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_ClassDenyAll(JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    public abstract String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_ClassDenyAllMethodPermitAll(
            JsonObject array);

    @POST
    @Path(CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    public abstract String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_ClassDenyAllMethodRolesAllowed(
            JsonObject array);

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + CLASS_DENY_ALL_PATH)
    public abstract ClassDenyAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBase_ClassDenyAll();

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE
            + CLASS_DENY_ALL_METHOD_PERMIT_ALL_PATH)
    public abstract ClassDenyAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBase_ClassDenyAllMethodPermitAll();

    @Path(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE
            + CLASS_DENY_ALL_METHOD_ROLES_ALLOWED_PATH)
    public abstract ClassDenyAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBase_ClassDenyAllMethodRolesAllowed();
}
