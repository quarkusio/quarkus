package io.quarkus.arc.test.buildextension.qualifiers;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.processor.QualifierRegistrar;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Singleton;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class AdditionalQualifiersTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(ToBeQualifier.class,
                    ToBeQualifierWithBindingField.class,
                    ToBeQualifierWithNonBindingField.class, Alpha.class, Bravo.class, Charlie.class)
            .qualifierRegistrars(new QualifierRegistrar() {

                @Override
                public Map<DotName, Set<String>> getAdditionalQualifiers() {
                    Map<DotName, Set<String>> qualifiers = new HashMap<>();
                    qualifiers.put(DotName.createSimple(ToBeQualifier.class.getName()), Collections.emptySet());
                    qualifiers.put(DotName.createSimple(ToBeQualifierWithNonBindingField.class.getName()),
                            Collections.singleton("foo"));
                    qualifiers.put(DotName.createSimple(ToBeQualifierWithBindingField.class.getName()),
                            Collections.singleton("value"));
                    return qualifiers;
                }
            })
            .build();

    @SuppressWarnings("serial")
    @Test
    public void testQualifierWasRegistered() {
        ArcContainer container = Arc.container();
        assertTrue(container.select(Alpha.class).isUnsatisfied());
        assertTrue(container.select(Alpha.class, new ToBeQualifierLiteral() {
        }).isResolvable());
        assertTrue(container.select(Bravo.class).isUnsatisfied());
        assertTrue(container.select(Bravo.class, new ToBeQualifierWithNonBindingFieldLiteral() {
            @Override
            public String foo() {
                return "blik";
            }

        }).isResolvable());
        assertTrue(container.select(Charlie.class).isUnsatisfied());
        assertTrue(container.select(Charlie.class, new ToBeQualifierWithBindingFieldLiteral() {
            @Override
            public String value() {
                return "ignored";
            }

            @Override
            public int age() {
                return 10;
            }

        }).isResolvable());
        assertTrue(container.select(Charlie.class, new ToBeQualifierWithBindingFieldLiteral() {
            @Override
            public String value() {
                return "ignored";
            }

            @Override
            public int age() {
                // this should be binding!
                return 1;
            }

        }).isUnsatisfied());
    }

    @ToBeQualifier
    @Singleton
    static class Alpha {

    }

    @ToBeQualifierWithNonBindingField(foo = "bzzzz")
    @Singleton
    static class Bravo {

    }

    // value should be ignore but age is used during resolution
    @ToBeQualifierWithBindingField(value = "bzzzz", age = 20)
    @ToBeQualifierWithBindingField(value = "nok", age = 10)
    @Singleton
    static class Charlie {

    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface ToBeQualifier {

    }

    @SuppressWarnings("serial")
    public abstract class ToBeQualifierLiteral extends AnnotationLiteral<ToBeQualifier> implements ToBeQualifier {
    }

    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface ToBeQualifierWithNonBindingField {
        String foo();
    }

    @SuppressWarnings("serial")
    public abstract class ToBeQualifierWithNonBindingFieldLiteral extends AnnotationLiteral<ToBeQualifierWithNonBindingField>
            implements ToBeQualifierWithNonBindingField {
    }

    @Repeatable(ToBeQualifierWithBindingField.Container.class)
    @Target({ TYPE, METHOD, FIELD, PARAMETER })
    @Retention(RUNTIME)
    public @interface ToBeQualifierWithBindingField {

        String value();

        int age();

        @Retention(RetentionPolicy.RUNTIME)
        @Target({ TYPE, METHOD, FIELD, PARAMETER })
        public @interface Container {

            ToBeQualifierWithBindingField[] value();

        }
    }

    @SuppressWarnings("serial")
    public abstract class ToBeQualifierWithBindingFieldLiteral extends AnnotationLiteral<ToBeQualifierWithBindingField>
            implements ToBeQualifierWithBindingField {
    }

}
