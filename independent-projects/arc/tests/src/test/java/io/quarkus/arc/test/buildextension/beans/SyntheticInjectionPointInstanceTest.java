package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInjectionPointInstanceTest {

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

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configure(List.class)
                    // List, List<String>
                    .addType(Type.create(DotName.createSimple(List.class.getName()), Kind.CLASS))
                    .addType(ParameterizedType.create(DotName.createSimple(List.class.getName()),
                            new Type[] { Type.create(DotName.createSimple(String.class.getName()), Kind.CLASS) }, null))
                    .creator(ListCreator.class)
                    .addInjectionPoint(ParameterizedType.create(DotName.createSimple(Instance.class),
                            new Type[] { ClassType.create(DotName.createSimple(SomethingRemovable.class)) }, null))
                    .unremovable()
                    .done();
        }

    }

    @Singleton
    static class SomethingRemovable {

        static final String STR = "I'm still here!";

        @Override
        public String toString() {
            return STR;
        }

    }

    public static class ListCreator implements BeanCreator<List<String>> {

        @SuppressWarnings("serial")
        @Override
        public List<String> create(SyntheticCreationalContext<List<String>> context) {
            return List.of(context.getInjectedReference(new TypeLiteral<Instance<SomethingRemovable>>() {
            }).get().toString());
        }

    }

}
