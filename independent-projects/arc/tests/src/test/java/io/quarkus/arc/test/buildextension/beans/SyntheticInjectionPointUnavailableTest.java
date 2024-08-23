package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.util.TypeLiteral;

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

public class SyntheticInjectionPointUnavailableTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new TestRegistrar()).build();

    @SuppressWarnings("serial")
    @Test
    public void testValidationFails() {
        assertThrows(CreationException.class, () -> Arc.container().instance(new TypeLiteral<List<String>>() {
        }).get());
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
                    .addInjectionPoint(ClassType.create(DotName.createSimple(BeanManager.class)))
                    .unremovable()
                    .done();
        }

    }

    public static class ListCreator implements BeanCreator<List<String>> {

        @Override
        public List<String> create(SyntheticCreationalContext<List<String>> context) {
            return List.of(context.getInjectedReference(BigDecimal.class).toString());
        }

    }

}
