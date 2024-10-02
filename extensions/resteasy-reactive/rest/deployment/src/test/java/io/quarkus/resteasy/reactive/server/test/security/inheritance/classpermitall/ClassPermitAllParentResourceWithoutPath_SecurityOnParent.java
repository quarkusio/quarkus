package io.quarkus.resteasy.reactive.server.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_INTERFACE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_PARENT;

import jakarta.annotation.security.PermitAll;

import io.vertx.core.json.JsonObject;

@PermitAll
public abstract class ClassPermitAllParentResourceWithoutPath_SecurityOnParent
        implements ClassPermitAllInterfaceWithPath_SecurityOnParent {

    @Override
    public ClassPermitAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_ClassPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_PARENT + CLASS_PERMIT_ALL_PATH);
    }

    @PermitAll
    @Override
    public ClassPermitAllSubResourceWithoutPath classPathOnInterface_SubDeclaredOnInterface_SubImplOnParent_ClassPermitAllMethodPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(CLASS_PATH_ON_INTERFACE + SUB_DECLARED_ON_INTERFACE
                + SUB_IMPL_ON_PARENT + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH);
    }

    @Override
    public String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnInterface_ImplOnParent_InterfaceMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_INTERFACE + IMPL_ON_PARENT + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }
}
