package io.quarkus.resteasy.reactive.server.test.security.inheritance.classpermitall;

import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PATH_ON_PARENT_RESOURCE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.CLASS_PERMIT_ALL_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.IMPL_ON_BASE;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.INTERFACE_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.PARENT_METHOD_WITH_PATH;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_DECLARED_ON_PARENT;
import static io.quarkus.resteasy.reactive.server.test.security.inheritance.SubPaths.SUB_IMPL_ON_BASE;

import jakarta.annotation.security.PermitAll;

import io.vertx.core.json.JsonObject;

@PermitAll
public class ClassPermitAllBaseResourceWithoutPathExtParentRes_SecurityOnBase
        extends ClassPermitAllParentResourceWithPath_SecurityOnBase {

    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnParentResource_ImplOnBase_InterfaceMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + INTERFACE_METHOD_WITH_PATH
                + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_ClassPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_PERMIT_ALL_PATH;
    }

    @PermitAll
    @Override
    public String classPathOnParentResource_ImplOnBase_ParentMethodWithPath_ClassPermitAllMethodPermitAll(JsonObject array) {
        return CLASS_PATH_ON_PARENT_RESOURCE + IMPL_ON_BASE + PARENT_METHOD_WITH_PATH + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH;
    }

    @Override
    public ClassPermitAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBase_ClassPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(
                CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT + SUB_IMPL_ON_BASE + CLASS_PERMIT_ALL_PATH);
    }

    @PermitAll
    @Override
    public ClassPermitAllSubResourceWithoutPath classPathOnParentResource_SubDeclaredOnParent_SubImplOnBase_ClassPermitAllMethodPermitAll() {
        return new ClassPermitAllSubResourceWithoutPath(CLASS_PATH_ON_PARENT_RESOURCE + SUB_DECLARED_ON_PARENT
                + SUB_IMPL_ON_BASE + CLASS_PERMIT_ALL_METHOD_PERMIT_ALL_PATH);
    }
}
