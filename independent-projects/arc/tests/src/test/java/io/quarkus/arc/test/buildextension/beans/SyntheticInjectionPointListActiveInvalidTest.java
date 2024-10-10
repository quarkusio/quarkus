package io.quarkus.arc.test.buildextension.beans;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import jakarta.enterprise.inject.spi.DefinitionException;
import jakarta.enterprise.util.TypeLiteral;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.ParameterizedType;
import org.jboss.jandex.WildcardType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Active;
import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.arc.test.ArcTestContainer;

public class SyntheticInjectionPointListActiveInvalidTest {
    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanRegistrars(context -> {
                context.configure(SyntheticBean.class)
                        .types(ClassType.create(SyntheticBean.class))
                        .creator(SynthBeanCreator.class)
                        .addInjectionPoint(ParameterizedType.create(List.class, WildcardType.UNBOUNDED),
                                AnnotationInstance.builder(Active.class).build())
                        .unremovable()
                        .done();
            })
            .shouldFail()
            .build();

    @Test
    public void trigger() {
        Throwable error = container.getFailure();
        assertNotNull(error);
        assertInstanceOf(DefinitionException.class, error);
        assertTrue(error.getMessage().contains(
                "Wildcard is not a legal type argument for a synthetic @Active List<?> injection point"));
    }

    static class SyntheticBean {
        SyntheticBean(List<?> list) {
        }
    }

    static class SynthBeanCreator implements BeanCreator<SyntheticBean> {
        @Override
        public SyntheticBean create(SyntheticCreationalContext<SyntheticBean> context) {
            return new SyntheticBean(context.getInjectedReference(new TypeLiteral<>() {
            }, Active.Literal.INSTANCE));
        }
    }
}
