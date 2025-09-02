package io.quarkus.hibernate.orm.enhancer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import jakarta.inject.Inject;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OrderColumn;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.hibernate.annotations.SortNatural;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.TransactionTestUtils;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusUnitTest;

/**
 * Checks that the missing @Embeddable check doesn't mistakely report
 * types that are annotated with @Embeddable (https://github.com/quarkusio/quarkus/issues/35598)
 * or generic type parameters on @Embedded field types (https://github.com/quarkusio/quarkus/issues/36065)
 * or overriden getters annotated with @EmbeddedId/@Embedded where the supertype getter returns a type not annotated
 * with @Embeddable
 * (https://github.com/quarkusio/quarkus/issues/36421).
 */
public class HibernateEntityEnhancerPresentEmbeddableTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(TransactionTestUtils.class)
                    .addClasses(EntityWithEmbedded.class, EmbeddableWithAnnotation.class,
                            ExtendedEmbeddableWithAnnotation.class,
                            NestingEmbeddableWithAnnotation.class,
                            GenericEmbeddableWithAnnotation.class,
                            EntityWithEmbeddedId.class, EntityWithEmbeddedIdAndOverriddenGetter.class,
                            EmbeddableIdWithAnnotation.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-orm.implicit-naming-strategy", "component-path");

    @Inject
    EntityManager em;

    // Just test that the generic embeddeds work correctly over a persist/retrieve cycle
    @Test
    public void embedded_smokeTest() {
        Long id = QuarkusTransaction.requiringNew().call(() -> {
            EntityWithEmbedded entity = new EntityWithEmbedded();
            entity.setName("name");
            entity.setEmbeddedWithAnnotation(new EmbeddableWithAnnotation("simple"));
            entity.setExtendedEmbeddedWithAnnotation(new ExtendedEmbeddableWithAnnotation("extended", 42));
            var nesting = new NestingEmbeddableWithAnnotation("nesting");
            entity.setNestingEmbeddedWithAnnotation(nesting);
            nesting.setEmbedded(new EmbeddableWithAnnotation("nested"));
            entity.setGenericEmbeddedWithAnnotation(new GenericEmbeddableWithAnnotation<>("generic"));
            entity.setEmbeddableListWithAnnotation(List.of(
                    new EmbeddableWithAnnotation("list1"),
                    new EmbeddableWithAnnotation("list2")));
            entity.setEmbeddableMapValueWithAnnotation(new TreeMap<>(Map.of(
                    "first", new EmbeddableWithAnnotation("map1"),
                    "second", new EmbeddableWithAnnotation("map2"))));
            em.persist(entity);
            return entity.getId();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            EntityWithEmbedded entity = em.find(EntityWithEmbedded.class, id);
            assertThat(entity).extracting(e -> e.getName())
                    .isEqualTo("name");
            assertThat(entity).extracting(e -> e.getEmbeddedWithAnnotation().getText())
                    .isEqualTo("simple");
            assertThat(entity).extracting(e -> e.getExtendedEmbeddedWithAnnotation().getText())
                    .isEqualTo("extended");
            assertThat(entity).extracting(e -> e.getExtendedEmbeddedWithAnnotation().getInteger())
                    .isEqualTo(42);
            assertThat(entity).extracting(e -> e.getNestingEmbeddedWithAnnotation().getText())
                    .isEqualTo("nesting");
            assertThat(entity).extracting(e -> e.getNestingEmbeddedWithAnnotation().getEmbedded().getText())
                    .isEqualTo("nested");
            assertThat(entity).extracting(e -> e.getGenericEmbeddedWithAnnotation().getValue())
                    .isEqualTo("generic");
            assertThat(entity).extracting(e -> e.getEmbeddableListWithAnnotation())
                    .asInstanceOf(InstanceOfAssertFactories.list(EmbeddableWithAnnotation.class))
                    .extracting(EmbeddableWithAnnotation::getText)
                    .containsExactly("list1", "list2");
            assertThat(entity).extracting(e -> e.getEmbeddableMapValueWithAnnotation())
                    .asInstanceOf(InstanceOfAssertFactories.map(String.class, EmbeddableWithAnnotation.class))
                    .extractingFromEntries(e -> e.getValue().getText())
                    .containsExactly("map1", "map2");
        });
    }

    // Just test that the embeddedIds work correctly over a persist/retrieve cycle
    @Test
    public void embeddedId_smokeTest() {
        QuarkusTransaction.requiringNew().run(() -> {
            EntityWithEmbeddedId entity1 = new EntityWithEmbeddedId();
            entity1.setId(new EmbeddableIdWithAnnotation("1"));
            em.persist(entity1);
        });

        QuarkusTransaction.requiringNew().run(() -> {
            EntityWithEmbeddedId entity = em.find(EntityWithEmbeddedId.class, new EmbeddableIdWithAnnotation("1"));
            assertThat(entity).isNotNull();
        });
    }

    @Test
    public void embeddedIdAndOverriddenGetter_smokeTest() {
        QuarkusTransaction.requiringNew().run(() -> {
            EntityWithEmbeddedIdAndOverriddenGetter entity1 = new EntityWithEmbeddedIdAndOverriddenGetter();
            entity1.setId(new EmbeddableIdWithAnnotation("2"));
            em.persist(entity1);
        });

        QuarkusTransaction.requiringNew().run(() -> {
            EntityWithEmbeddedIdAndOverriddenGetter entity = em.find(EntityWithEmbeddedIdAndOverriddenGetter.class,
                    new EmbeddableIdWithAnnotation("2"));
            assertThat(entity).isNotNull();
        });
    }

    @Entity
    public static class EntityWithEmbedded {

        @Id
        @GeneratedValue
        private Long id;

        private String name;

        @Embedded
        private EmbeddableWithAnnotation embeddedWithAnnotation;

        @Embedded
        private ExtendedEmbeddableWithAnnotation extendedEmbeddedWithAnnotation;

        @Embedded
        private NestingEmbeddableWithAnnotation nestingEmbeddedWithAnnotation;

        @Embedded
        private GenericEmbeddableWithAnnotation<String> genericEmbeddedWithAnnotation;

        @ElementCollection
        @OrderColumn
        private List<EmbeddableWithAnnotation> embeddableListWithAnnotation;

        @ElementCollection
        @SortNatural
        private SortedMap<String, EmbeddableWithAnnotation> embeddableMapValueWithAnnotation;

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

        public EmbeddableWithAnnotation getEmbeddedWithAnnotation() {
            return embeddedWithAnnotation;
        }

        public void setEmbeddedWithAnnotation(EmbeddableWithAnnotation embeddedWithAnnotation) {
            this.embeddedWithAnnotation = embeddedWithAnnotation;
        }

        public ExtendedEmbeddableWithAnnotation getExtendedEmbeddedWithAnnotation() {
            return extendedEmbeddedWithAnnotation;
        }

        public void setExtendedEmbeddedWithAnnotation(ExtendedEmbeddableWithAnnotation extendedEmbeddedWithAnnotation) {
            this.extendedEmbeddedWithAnnotation = extendedEmbeddedWithAnnotation;
        }

        public NestingEmbeddableWithAnnotation getNestingEmbeddedWithAnnotation() {
            return nestingEmbeddedWithAnnotation;
        }

        public void setNestingEmbeddedWithAnnotation(NestingEmbeddableWithAnnotation nestingEmbeddedWithAnnotation) {
            this.nestingEmbeddedWithAnnotation = nestingEmbeddedWithAnnotation;
        }

        public GenericEmbeddableWithAnnotation<String> getGenericEmbeddedWithAnnotation() {
            return genericEmbeddedWithAnnotation;
        }

        public void setGenericEmbeddedWithAnnotation(GenericEmbeddableWithAnnotation<String> genericEmbeddedWithAnnotation) {
            this.genericEmbeddedWithAnnotation = genericEmbeddedWithAnnotation;
        }

        public List<EmbeddableWithAnnotation> getEmbeddableListWithAnnotation() {
            return embeddableListWithAnnotation;
        }

        public void setEmbeddableListWithAnnotation(List<EmbeddableWithAnnotation> embeddableListWithAnnotation) {
            this.embeddableListWithAnnotation = embeddableListWithAnnotation;
        }

        public Map<String, EmbeddableWithAnnotation> getEmbeddableMapValueWithAnnotation() {
            return embeddableMapValueWithAnnotation;
        }

        public void setEmbeddableMapValueWithAnnotation(
                SortedMap<String, EmbeddableWithAnnotation> embeddableMapValueWithAnnotation) {
            this.embeddableMapValueWithAnnotation = embeddableMapValueWithAnnotation;
        }
    }

    @Embeddable
    public static class EmbeddableWithAnnotation {
        private String text;

        protected EmbeddableWithAnnotation() {
            // For Hibernate ORM only - it will change the property value through reflection
        }

        public EmbeddableWithAnnotation(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    @Embeddable
    public static class NestingEmbeddableWithAnnotation {
        private String text;

        @Embedded
        private EmbeddableWithAnnotation embedded;

        protected NestingEmbeddableWithAnnotation() {
            // For Hibernate ORM only - it will change the property value through reflection
        }

        public NestingEmbeddableWithAnnotation(String text) {
            this.text = text;
            this.embedded = new EmbeddableWithAnnotation(text);
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public EmbeddableWithAnnotation getEmbedded() {
            return embedded;
        }

        public void setEmbedded(EmbeddableWithAnnotation embedded) {
            this.embedded = embedded;
        }
    }

    @MappedSuperclass
    public static abstract class MappedSuperclassForEmbeddable {
        private String text;

        protected MappedSuperclassForEmbeddable() {
            // For Hibernate ORM only - it will change the property value through reflection
        }

        public MappedSuperclassForEmbeddable(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    @Embeddable
    public static class ExtendedEmbeddableWithAnnotation extends MappedSuperclassForEmbeddable {
        private Integer integer;

        protected ExtendedEmbeddableWithAnnotation() {
            // For Hibernate ORM only - it will change the property value through reflection
        }

        public ExtendedEmbeddableWithAnnotation(String text, Integer integer) {
            super(text);
            this.integer = integer;
        }

        public Integer getInteger() {
            return integer;
        }

        public void setInteger(Integer integer) {
            this.integer = integer;
        }
    }

    @Embeddable
    public static class GenericEmbeddableWithAnnotation<T> {
        private T value;

        protected GenericEmbeddableWithAnnotation() {
            // For Hibernate ORM only - it will change the property value through reflection
        }

        public GenericEmbeddableWithAnnotation(T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
    }

    @Entity
    public static class EntityWithEmbeddedId {
        @EmbeddedId
        private EmbeddableIdWithAnnotation id;

        public EmbeddableIdWithAnnotation getId() {
            return id;
        }

        public void setId(EmbeddableIdWithAnnotation id) {
            this.id = id;
        }
    }

    public interface Identifiable {
        Object getId();
    }

    @Entity
    public static class EntityWithEmbeddedIdAndOverriddenGetter implements Identifiable {
        private EmbeddableIdWithAnnotation id;

        @Override
        @EmbeddedId
        public EmbeddableIdWithAnnotation getId() {
            return id;
        }

        public void setId(EmbeddableIdWithAnnotation id) {
            this.id = id;
        }
    }

    @Embeddable
    public static class EmbeddableIdWithAnnotation {
        private String text;

        protected EmbeddableIdWithAnnotation() {
            // For Hibernate ORM only - it will change the property value through reflection
        }

        public EmbeddableIdWithAnnotation(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

}
