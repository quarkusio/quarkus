package io.quarkus.hibernate.orm.enhancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that a missing @Embeddable is reported as failure at the build stage rather than a cryptic error at the runtime.
 * See https://github.com/quarkusio/quarkus/issues/35598
 */
class HibernateEntityEnhancerMissingEmbeddableAnnotationTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TransactionTestUtils.class)
                    .addClasses(
                            EntityWithEmbedded.class,
                            EntityWithEmbedded.EmbeddableMissingAnnotation.class,
                            EntityWithEmbedded.EmbeddableWithAnnotation.class))
            .withConfigurationResource("application.properties")
            .assertException(ex -> assertThat(ex)
                    .isNotNull()
                    .hasMessageContainingAll(
                            "Type " + EntityWithEmbedded.EmbeddableMissingAnnotation.class.getName(),
                            "must be annotated with @Embeddable, because it is used as an embeddable",
                            "This type is used in class " + EntityWithEmbedded.EmbeddableWithAnnotation.class.getName(),
                            "for attribute ", "embeddableMissingAnnotation"));

    // Just test that the embedded non-ID works correctly over a persist/retrieve cycle
    @Test
    void test() throws Exception {
        fail();
    }

    @Entity
    public static class EntityWithEmbedded {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        @Embedded
        private EmbeddableWithAnnotationExtended embedded;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public EmbeddableWithAnnotationExtended getEmbedded() {
            return embedded;
        }

        public void setEmbedded(EmbeddableWithAnnotationExtended otherEmbedded) {
            this.embedded = otherEmbedded;
        }

        // @Embeddable // is missing on this class
        public static class EmbeddableMissingAnnotation {
            private String string;

            public EmbeddableMissingAnnotation() {
            }

            public EmbeddableMissingAnnotation(String string) {
                this.string = string;
            }

            public String getString() {
                return string;
            }

            public void setString(String string) {
                this.string = string;
            }
        }

        @Embeddable
        public static class EmbeddableWithAnnotation {
            private String text;

            @Embedded
            private EmbeddableMissingAnnotation embeddableMissingAnnotation;

            protected EmbeddableWithAnnotation() {
                // For Hibernate ORM only - it will change the property value through reflection
            }

            public EmbeddableWithAnnotation(String text) {
                this.text = text;
                this.embeddableMissingAnnotation = new EmbeddableMissingAnnotation(text);
            }

            public String getText() {
                return text;
            }

            public void setText(String mutableProperty) {
                this.text = mutableProperty;
            }

            public EmbeddableMissingAnnotation getEmbeddableMissingAnnotation() {
                return embeddableMissingAnnotation;
            }

            public void setEmbeddableMissingAnnotation(EmbeddableMissingAnnotation embeddableMissingAnnotation) {
                this.embeddableMissingAnnotation = embeddableMissingAnnotation;
            }
        }

        @Embeddable
        public static class EmbeddableWithAnnotationExtended extends EmbeddableWithAnnotation {
            private Integer integer;

            public Integer getInteger() {
                return integer;
            }

            public void setInteger(Integer integer) {
                this.integer = integer;
            }
        }
    }

}
