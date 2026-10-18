package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInjectionPointValidationTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .shouldFail()
            .beanRegistrars(new TestRegistrar()).build();

    @Test
    public void testValidationFails() {
        assertNotNull(container.getFailure());
        assertTrue(container.getFailure().getMessage().contains(
                "Unsatisfied dependency for type java.math.BigInteger and qualifiers [@jakarta.enterprise.inject.Default]"),
                container.getFailure().getMessage());
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
                    // This injection point is not satisfied
                    .addInjectionPoint(ClassType.create(DotName.createSimple(BigInteger.class)))
                    .unremovable()
                    .done();
        }

    }

    public static class ListCreator implements BeanCreator<List<String>> {

        @Override
        public List<String> create(CreationalContext<List<String>> creationalContext, Map<String, Object> params) {
            return List.of();
        }

    }

}
