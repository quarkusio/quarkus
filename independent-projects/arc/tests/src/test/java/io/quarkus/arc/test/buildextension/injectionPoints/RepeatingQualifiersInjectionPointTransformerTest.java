package io.quarkus.arc.test.buildextension.injectionPoints;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.processor.InjectionPointsTransformer;
import io.quarkus.arc.test.ArcTestContainer;

public class RepeatingQualifiersInjectionPointTransformerTest {

    public static final String FIRST_STRING = "neverwhere";
    public static final String SECOND_STRING = "london";

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Foo.class, Bar.class, Location.class, Locations.class, ShapeableBean.class)
            .injectionPointsTransformers(new MyTransformer())
            .build();

    @Test
    public void testQualifiersHandledCorrectly() {
        Assertions.assertTrue(Arc.container().select(ShapeableBean.class).isResolvable());
    }

    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface Locations {
        Location[] value();
    }

    @Qualifier
    @Inherited
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    @Repeatable(Locations.class)
    public @interface Location {
        String value();

        class Literal extends AnnotationLiteral<Location> implements Location {

            private final String value;

            public Literal(String value) {
                this.value = value;
            }

            @Override
            public String value() {
                return value;
            }
        }
    }

    @Singleton
    public static class Foo {

    }

    @Singleton
    @Location(FIRST_STRING)
    @Location(SECOND_STRING)
    public static class Bar {

    }

    @Singleton
    public static class ShapeableBean {

        // Bar bean exists only with repeated qualifiers - transformer adds those
        // Foo bean exists only without qualifiers - transformer removes all qualifiers
        public ShapeableBean(@Location("doesn't") @Location("matter") Foo foo, Bar bar) {

        }
    }

    static class MyTransformer implements InjectionPointsTransformer {

        @Override
        public boolean appliesTo(Type requiredType) {
            // applies to all Foo/Bar injection points
            return requiredType.equals(ClassType.create(Foo.class)) || requiredType.equals(ClassType.create(Bar.class));
        }

        @Override
        public void transform(TransformationContext transformationContext) {
            if (AnnotationTarget.Kind.METHOD_PARAMETER == transformationContext.getAnnotationTarget().kind()) {
                if (transformationContext.getAnnotationTarget().asMethodParameter().type().name()
                        .equals(DotName.createSimple(Foo.class))) {
                    transformationContext.transform().removeAll().done();
                } else {
                    // add repeating qualifiers
                    transformationContext.transform()
                            .add(AnnotationInstance.builder(Location.class).value(FIRST_STRING).build())
                            .add(AnnotationInstance.builder(Location.class).value(SECOND_STRING).build())
                            .done();
                }
            } else {
                throw new IllegalStateException(
                        "Unexpected injection point kind: " + transformationContext.getAnnotationTarget().kind());
            }
        }
    }
}
