package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
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
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInjectionPointMetadataTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(MyDependentFoo.class, Consumer.class)
            .beanRegistrars(new TestRegistrar()).build();

    @SuppressWarnings("resource")
    @Test
    public void testMetadata() {
        List<MyDependentFoo> list = Arc.container().instance(Consumer.class).get().list;
        assertNotNull(list);
        MyDependentFoo foo = list.get(0);
        assertNotNull(foo.injectionPoint);
        assertEquals(InjectableBean.Kind.SYNTHETIC, ((InjectableBean<?>) foo.injectionPoint.getBean()).getKind());
        assertNull(foo.injectionPoint.getMember());
        assertNotNull(foo.syntheticMetada);
        assertEquals(Consumer.class, foo.syntheticMetada.getMember().getDeclaringClass());
        assertEquals(InjectableBean.Kind.CLASS, ((InjectableBean<?>) foo.syntheticMetada.getBean()).getKind());
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configure(List.class)
                    // List, List<MyDependentFoo>
                    .addType(Type.create(DotName.createSimple(List.class.getName()), Kind.CLASS))
                    .addType(ParameterizedType.create(DotName.createSimple(List.class),
                            new Type[] { ClassType.create(DotName.createSimple(MyDependentFoo.class)) }, null))
                    .creator(ListCreator.class)
                    .addInjectionPoint(ClassType.create(DotName.createSimple(MyDependentFoo.class)))
                    .addInjectionPoint(ClassType.create(DotName.createSimple(InjectionPoint.class)))
                    .unremovable()
                    .done();
        }

    }

    @Singleton
    public static class Consumer {

        @Inject
        List<MyDependentFoo> list;

    }

    @Dependent
    public static class MyDependentFoo {

        InjectionPoint syntheticMetada;

        @Inject
        InjectionPoint injectionPoint;

    }

    public static class ListCreator implements BeanCreator<List<MyDependentFoo>> {

        @Override
        public List<MyDependentFoo> create(SyntheticCreationalContext<List<MyDependentFoo>> context) {
            MyDependentFoo foo = context.getInjectedReference(MyDependentFoo.class);
            foo.syntheticMetada = context.getInjectedReference(InjectionPoint.class);
            return List.of(foo);
        }

    }

}
