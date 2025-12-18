package io.quarkus.arc.processor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Collections;

import jakarta.annotation.Priority;
import jakarta.decorator.Decorator;
import jakarta.decorator.Delegate;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.inject.Inject;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Index;
import org.jboss.jandex.IndexView;
import org.junit.jupiter.api.Test;

public class AbstractDecoratorValidationTest {

    interface DecoratedInterface {
        void foo();
    }

    @Test
    public void testAbstractDecoratorValid() throws IOException {
        IndexView index = Index.of(
                DecoratedInterface.class,
                InterfaceDecoratorValid.class);

        BeanDeployment deployment = BeanProcessor.builder()
                .setImmutableBeanArchiveIndex(index)
                .build()
                .getBeanDeployment();

        assertNotNull(createDecorator(index, deployment, InterfaceDecoratorValid.class));
    }

    @Test
    public void testAbstractDecoratorWithIllegalAbstractMethod() throws IOException {
        IndexView index = Index.of(
                DecoratedInterface.class,
                InterfaceDecoratorInvalid.class);

        BeanDeployment deployment = BeanProcessor.builder()
                .setImmutableBeanArchiveIndex(index)
                .build()
                .getBeanDeployment();

        assertThrows(DefinitionException.class,
                () -> createDecorator(index, deployment, InterfaceDecoratorInvalid.class));
    }

    @Test
    public void testInheritedAbstractMethodIsAllowed() throws IOException {
        IndexView index = Index.of(
                DecoratedInterface.class,
                AbstractBaseDecorator.class,
                InheritingDecorator.class);

        BeanDeployment deployment = BeanProcessor.builder()
                .setImmutableBeanArchiveIndex(index)
                .build()
                .getBeanDeployment();

        assertNotNull(createDecorator(index, deployment, InheritingDecorator.class));
    }

    private Object createDecorator(IndexView index, BeanDeployment deployment, Class<?> clazz) {
        ClassInfo classInfo = index.getClassByName(
                DotName.createSimple(clazz.getName()));
        return Decorators.createDecorator(
                classInfo,
                deployment,
                new InjectionPointModifier(Collections.emptyList(), null));
    }

    @Dependent
    @Priority(1)
    @Decorator
    abstract static class InterfaceDecoratorValid implements DecoratedInterface {

        @Inject
        @Delegate
        DecoratedInterface delegate;

        @Override
        public abstract void foo();
    }

    @Dependent
    @Priority(1)
    @Decorator
    abstract static class InterfaceDecoratorInvalid implements DecoratedInterface {

        @Inject
        @Delegate
        DecoratedInterface delegate;

        @Override
        public abstract void foo();

        abstract void extra();
    }

    abstract static class AbstractBaseDecorator {

        abstract void inherited();
    }

    @Dependent
    @Priority(1)
    @Decorator
    abstract static class InheritingDecorator extends AbstractBaseDecorator
            implements DecoratedInterface {

        @Inject
        @Delegate
        DecoratedInterface delegate;

        @Override
        public abstract void foo();
    }
}
