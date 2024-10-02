package io.quarkus.resteasy.reactive.server.test.security.inheritance.multiple.pathonbase;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.MULTIPLE_INHERITANCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.THIRD_INTERFACE;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

public interface BaseResource_Third_Interface {

    @POST
    @Path(MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + THIRD_INTERFACE + INTERFACE_METHOD_WITH_PATH
            + METHOD_ROLES_ALLOWED_PATH)
    String multipleInheritance_ClassPathOnBase_ImplOnBase_ThirdInterface_InterfaceMethodWithPath_MethodRolesAllowed(
            JsonObject array);

    @POST
    @Path(MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + THIRD_INTERFACE + INTERFACE_METHOD_WITH_PATH
            + NO_SECURITY_ANNOTATION_PATH)
    String multipleInheritance_ClassPathOnBase_ImplOnBase_ThirdInterface_InterfaceMethodWithPath_NoAnnotation(
            JsonObject array);

    @RolesAllowed("admin")
    @POST
    @Path(MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + THIRD_INTERFACE + INTERFACE_METHOD_WITH_PATH
            + METHOD_ROLES_ALLOWED_PATH)
    default String multipleInheritance_ClassPathOnBase_ImplOnInterface_ThirdInterface_InterfaceMethodWithPath_MethodRolesAllowed(
            JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + THIRD_INTERFACE + INTERFACE_METHOD_WITH_PATH
                + METHOD_ROLES_ALLOWED_PATH;
    }

    @POST
    @Path(MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + THIRD_INTERFACE + INTERFACE_METHOD_WITH_PATH
            + NO_SECURITY_ANNOTATION_PATH)
    default String multipleInheritance_ClassPathOnBase_ImplOnInterface_ThirdInterface_InterfaceMethodWithPath_NoAnnotation(
            JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_INTERFACE + THIRD_INTERFACE + INTERFACE_METHOD_WITH_PATH
                + NO_SECURITY_ANNOTATION_PATH;
    }
}
