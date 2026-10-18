package io.quarkus.resteasy.test.security.inheritance.classdenyall;

import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_DENY_ALL_PREFIX;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.test.security.inheritance.SubPaths.CLASS_SECURITY_ON_INTERFACE;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Path(CLASS_DENY_ALL_PREFIX + CLASS_SECURITY_ON_INTERFACE + CLASS_PATH_ON_PARENT_RESOURCE)
public abstract class ClassDenyAllParentResourceWithPath_SecurityOnInterface
        implements ClassDenyAllInterfaceWithoutPath_PathOnParent_SecurityOnInterface {

}
