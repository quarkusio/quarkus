package io.quarkus.resteasy.reactive.server.test.security.inheritance.multiple.pathonbase;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.MULTIPLE_INHERITANCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SECOND_INTERFACE;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public interface BaseResource_Second_Interface
        extends BaseResource_Third_Interface {

    @POST
    @Path(MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + SECOND_INTERFACE + INTERFACE_METHOD_WITH_PATH
            + METHOD_ROLES_ALLOWED_PATH)
    String multipleInheritance_ClassPathOnBase_ImplOnBase_SecondInterface_InterfaceMethodWithPath_MethodRolesAllowed(
            JsonObject array);

    @POST
    @Path(MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + SECOND_INTERFACE + INTERFACE_METHOD_WITH_PATH
            + NO_SECURITY_ANNOTATION_PATH)
    String multipleInheritance_ClassPathOnBase_ImplOnBase_SecondInterface_InterfaceMethodWithPath_NoAnnotation(
            JsonObject array);

}
