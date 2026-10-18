package io.quarkus.resteasy.test.security.inheritance.classdenyall;

// must always be here as interface needs an implementing class, otherwise is ignored
public class ClassDenyAllBaseResourceWithoutPathImplInterface_SecurityOnInterface
        implements ClassDenyAllInterfaceWithPath_SecurityOnInterface {
}
