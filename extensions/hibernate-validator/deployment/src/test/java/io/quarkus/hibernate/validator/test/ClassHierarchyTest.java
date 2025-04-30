package io.quarkus.hibernate.validator.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractCollection;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.Validator;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;

public class ClassHierarchyTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest().setArchiveProducer(() -> {
        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addClass(Dto.class);
        // Create an inner class with an incomplete hierarchy
        try (DynamicType.Unloaded<?> superClass = new ByteBuddy()
                .subclass(Object.class)
                .name("SuperClass")
                .make();
                DynamicType.Unloaded<?> outerClass = new ByteBuddy()
                        .subclass(superClass.getTypeDescription())
                        .name("OuterClass")
                        .make();
                DynamicType.Unloaded<?> innerClass = new ByteBuddy()
                        .subclass(AbstractCollection.class)
                        .innerTypeOf(outerClass.getTypeDescription())
                        .name("InnerClass")
                        .make();
                DynamicType.Loaded<?> innerLoad = innerClass.load(Thread.currentThread().getContextClassLoader())) {
            javaArchive.add(new ByteArrayAsset(innerLoad.getBytes()), "InnerClass.class");
        }
        return javaArchive;
    });

    @Inject
    Validator validator;

    @Test
    public void doNotFailWhenLoadingIncompleteClassHierarchy() {
        assertThat(validator).isNotNull();
    }

    @Valid
    public static class Dto {
        String name;

        // InnerClass is a subclass with an incomplete hierarchy
        @Valid
        AbstractCollection<String> items;
    }
}
