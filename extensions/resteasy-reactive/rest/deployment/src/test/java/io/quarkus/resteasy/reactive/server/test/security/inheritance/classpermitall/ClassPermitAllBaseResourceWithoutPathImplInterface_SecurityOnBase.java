package io.quarkus.resteasy.reactive.server.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.annotation.security.PermitAll;

import io.vertx.core.json.JsonObject;

@PermitAll
public class ClassPermitAllBaseResourceWithoutPathImplInterface_SecurityOnBase
        implements ClassPermitAllInterfaceWithPath_SecurityOnBase {

    @Override
    public String classPathOnInterface_ImplOnBase_ImplMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnInterface_ImplOnBase_ImplMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + IMPL_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnInterface_ImplOnBase_InterfaceMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public ClassPermitAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_ClassPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_BASE + CLASS_PERMIT_ALL_PATH);
    }

    @PermitAll
    @Override
    public ClassPermitAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnBase_ClassPermitAllMethodPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_BASE + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH);
    }
}
