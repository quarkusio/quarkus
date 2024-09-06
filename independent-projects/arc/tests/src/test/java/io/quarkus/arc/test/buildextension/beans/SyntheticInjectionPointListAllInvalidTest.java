package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.util.TypeLiteral;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.WildcardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.All;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.processor.BeanRegistrar;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInjectionPointListAllInvalidTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(new TestRegistrar())
            .shouldFail()
            .build();

    @Test
    public void testListAllInjection() {
        assertNotNull(container.getFailure());
        assertTrue(container.getFailure().getMessage().contains(
                "Wildcard is not a legal type argument for a synthetic @All List<?> injection point"));
    }

    static class SyntheticBean {

        public SyntheticBean(List<?> list) {
        }
    }

    static class TestRegistrar implements BeanRegistrar {

        @Override
        public void register(RegistrationContext context) {
            context.configure(SyntheticBean.class)
                    .addType(ClassType.create(DotName.createSimple(SyntheticBean.class)))
                    .creator(SynthBeanCreator.class)
                    // add injection point for @All List<?> - wildcard is NOT a legal bean type
                    .addInjectionPoint(ParameterizedType.create(List.class, WildcardType.UNBOUNDED),
                            AnnotationInstance.builder(All.class).build())
                    .unremovable()
                    .done();
        }

    }

    public static class SynthBeanCreator implements BeanCreator<SyntheticBean> {

        @Override
        public SyntheticBean create(SyntheticCreationalContext<SyntheticBean> context) {
            return new SyntheticBean(context.getInjectedReference(new TypeLiteral<List<?>>() {
            }, All.Literal.INSTANCE));
        }

    }
}
