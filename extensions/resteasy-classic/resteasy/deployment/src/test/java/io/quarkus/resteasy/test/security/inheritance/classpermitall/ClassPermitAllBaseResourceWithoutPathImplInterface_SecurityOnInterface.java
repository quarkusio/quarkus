package io.quarkus.resteasy.test.security.inheritance.classpermitall;

// must always be here as interface needs an implementing class, otherwise is ignored
public class ClassPermitAllBaseResourceWithoutPathImplInterface_SecurityOnInterface
        implements ClassPermitAllInterfaceWithPath_SecurityOnInterface {
}
