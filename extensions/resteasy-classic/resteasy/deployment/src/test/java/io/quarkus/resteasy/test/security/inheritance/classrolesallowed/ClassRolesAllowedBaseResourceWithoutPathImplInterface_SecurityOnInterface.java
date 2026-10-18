package io.quarkus.resteasy.test.security.inheritance.classrolesallowed;

// must always be here as interface needs an implementing class, otherwise is ignored
public class ClassRolesAllowedBaseResourceWithoutPathImplInterface_SecurityOnInterface
        implements ClassRolesAllowedInterfaceWithPath_SecurityOnInterface {
}
