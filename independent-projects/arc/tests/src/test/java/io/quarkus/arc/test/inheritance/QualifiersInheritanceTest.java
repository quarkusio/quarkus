package io.quarkus.arc.test.inheritance;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class QualifiersInheritanceTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder().removeUnusedBeans(false)
            .beanClasses(SuperBean.class, Alpha.class, Bravo.class, InheritedQualifier.class, NonInheritedQualifier.class)
            .build();

    @SuppressWarnings("serial")
    @Test
    public void testInheritance() {
        ArcContainer container = Arc.container();
        // Bravo is not eligible because it has @InheritedQualifier("bravo")
        assertTrue(container.select(SuperBean.class, new InheritedQualifier.Literal("super")).isResolvable());
        // @NonInheritedQualifier is not inherited
        assertFalse(container.select(SuperBean.class, new AnnotationLiteral<NonInheritedQualifier>() {
        }).isResolvable());
    }

    @InheritedQualifier("super")
    @NonInheritedQualifier
    static class SuperBean {

        public void ping() {
        }
    }

    // -> inherited @InheritedQualifier("super")
    @ApplicationScoped
    static class Alpha extends SuperBean {

    }

    @InheritedQualifier("bravo")
    @ApplicationScoped
    static class Bravo extends SuperBean {

    }

    @Inherited
    @Qualifier
    @Retention(RUNTIME)
    @interface InheritedQualifier {

        String value();

        @SuppressWarnings("serial")
        static class Literal extends AnnotationLiteral<InheritedQualifier> implements InheritedQualifier {

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

    @Qualifier
    @Retention(RUNTIME)
    @interface NonInheritedQualifier {

    }
}
