package io.quarkus.test.junit.launcher;

import static org.junit.platform.commons.util.ReflectionUtils.HierarchyTraversalMode.TOP_DOWN;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.ReflectionUtils;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;

class ClassTestDescriptor extends AbstractTestDescriptor {

    private final Class<?> testClass;

    public ClassTestDescriptor(Class<?> testClass, TestDescriptor parent) {
        super( //
                parent.getUniqueId().append("class", testClass.getName()), //
                testClass.getSimpleName(), //
                ClassSource.from(testClass) //
        );
        System.out.println("made test class dectipr for " + testClass);
        this.testClass = testClass;
        setParent(parent);
        addAllChildren();
    }

    private void debugprint(Object o) {
        System.out.println("HOLLY found field " + o);
    }

    private void addAllChildren() {

        // TODO what about templates, etc? We would like to delegate this to JUnit
        // Could this help us?  (new DiscoverySelectorResolver()).resolveSelectors(discoveryRequest, engineDescriptor);
        // No, it's internal
        Predicate<Method> isTestField = field -> AnnotationUtils.isAnnotated(field, Test.class);

        System.out.println("HOLLY adding children of " + testClass);
        ReflectionUtils
                .findMethods(testClass, isTestField, TOP_DOWN)
                .stream()
                .forEach(this::debugprint);

        ReflectionUtils
                .findMethods(testClass, isTestField, TOP_DOWN)
                .stream() //
                .map(field -> new FieldTestDescriptor(field, this)) //
                .forEach(this::addChild);
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }
}
