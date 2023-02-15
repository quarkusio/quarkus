package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.InjectionPoint;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.Type;
import org.jboss.jandex.Type.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInjectionPointMetadataInvalidTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new TestRegistrar())
            .shouldFail()
            .build();

    @Test
    public void testMetadataNotAvailable() {
        assertNotNull(container.getFailure());
        assertTrue(container.getFailure().getMessage().contains(
                "Only @Dependent beans can access metadata about an injection point:"));
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configure(List.class)
                    .addType(Type.create(DotName.createSimple(List.class.getName()), Kind.CLASS))
                    .addType(ParameterizedType.create(DotName.createSimple(List.class),
                            new Type[] { ClassType.create(DotName.createSimple(String.class)) }, null))
                    .creator(ListCreator.class)
                    .scope(ApplicationScoped.class)
                    // This is not legal!
                    .addInjectionPoint(ClassType.create(DotName.createSimple(InjectionPoint.class)))
                    .done();
        }

    }

    public static class ListCreator implements BeanCreator<List<String>> {

        @Override
        public List<String> create(SyntheticCreationalContext<List<String>> context) {
            return List.of(context.getInjectedReference(InjectionPoint.class).toString());
        }

    }

}
