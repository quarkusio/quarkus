package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInjectionPointUnremovableTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(SomethingRemovable.class)
            .removeUnusedBeans(true)
            .beanRegistrars(new TestRegistrar()).build();

    @SuppressWarnings("serial")
    @Test
    public void testBeanNotRemoved() {
        List<String> list = Arc.container().instance(new TypeLiteral<List<String>>() {
        }).get();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(SomethingRemovable.STR, list.get(0));
    }

    @Singleton
    static class SomethingRemovable {

        static final String STR = "I'm still here!";

        @Override
        public String toString() {
            return STR;
        }

    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configure(List.class)
                    // List, List<String>
                    .addType(ClassType.create(DotName.createSimple(List.class)))
                    .addType(ParameterizedType.create(DotName.createSimple(List.class),
                            new Type[] { ClassType.create(DotName.createSimple(String.class)) }, null))
                    .creator(ListCreator.class)
                    .addInjectionPoint(ClassType.create(DotName.createSimple(SomethingRemovable.class)))
                    .unremovable()
                    .done();
        }

    }

    public static class ListCreator implements BeanCreator<List<String>> {

        @Override
        public List<String> create(SyntheticCreationalContext<List<String>> context) {
            return List.of(context.getInjectedReference(SomethingRemovable.class).toString());
        }

    }

}
