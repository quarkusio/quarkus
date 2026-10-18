package io.quarkus.resteasy.reactive.server.test.security.inheritance.multiple.pathonbase;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_NO_ANNOTATION_PREFIX;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.FIRST_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.METHOD_ROLES_ALLOWED_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.MULTIPLE_INHERITANCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.NO_SECURITY_ANNOTATION_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SECOND_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.THIRD_INTERFACE;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import io.vertx.core.json.JsonObject;

@Path(CLASS_NO_ANNOTATION_PREFIX + MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE)
public class BaseResource implements BaseResource_First_Interface {

    @POST
    @RolesAllowed("admin")
    @Path(MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + METHOD_ROLES_ALLOWED_PATH)
    public String multipleInheritance_ClassPathOnBase_ImplOnBase_ImplWithPath_MethodRolesAllowed(JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH
                + METHOD_ROLES_ALLOWED_PATH;
    }

    @POST
    @Path(MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + NO_SECURITY_ANNOTATION_PATH)
    public String multipleInheritance_ClassPathOnBase_ImplOnBase_ImplWithPath_NoAnnotation(JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH
                + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String multipleInheritance_ClassPathOnBase_ImplOnBase_FirstInterface_InterfaceMethodWithPath_MethodRolesAllowed(
            JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + FIRST_INTERFACE + INTERFACE_METHOD_WITH_PATH
                + METHOD_ROLES_ALLOWED_PATH;
    }

    @Override
    public String multipleInheritance_ClassPathOnBase_ImplOnBase_FirstInterface_InterfaceMethodWithPath_NoAnnotation(
            JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + FIRST_INTERFACE + INTERFACE_METHOD_WITH_PATH
                + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String multipleInheritance_ClassPathOnBase_ImplOnBase_SecondInterface_InterfaceMethodWithPath_MethodRolesAllowed(
            JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + SECOND_INTERFACE + INTERFACE_METHOD_WITH_PATH
                + METHOD_ROLES_ALLOWED_PATH;
    }

    @Override
    public String multipleInheritance_ClassPathOnBase_ImplOnBase_SecondInterface_InterfaceMethodWithPath_NoAnnotation(
            JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + SECOND_INTERFACE + INTERFACE_METHOD_WITH_PATH
                + NO_SECURITY_ANNOTATION_PATH;
    }

    @RolesAllowed("admin")
    @Override
    public String multipleInheritance_ClassPathOnBase_ImplOnBase_ThirdInterface_InterfaceMethodWithPath_MethodRolesAllowed(
            JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + THIRD_INTERFACE + INTERFACE_METHOD_WITH_PATH
                + METHOD_ROLES_ALLOWED_PATH;
    }

    @Override
    public String multipleInheritance_ClassPathOnBase_ImplOnBase_ThirdInterface_InterfaceMethodWithPath_NoAnnotation(
            JsonObject array) {
        return MULTIPLE_INHERITANCE + CLASS_PATH_ON_RESOURCE + IMPL_ON_BASE + THIRD_INTERFACE + INTERFACE_METHOD_WITH_PATH
                + NO_SECURITY_ANNOTATION_PATH;
    }
}
