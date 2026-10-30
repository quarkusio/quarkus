package io.quarkus.hibernate.orm.applicationfieldaccess;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Registering a field writer makes generated strategies drop {@code final} from the field, so ORM only
 * registers writers for fields that may hold a persistent attribute.
 * <p>
 * Only {@code @Transient} fields are asserted here. Hibernate's own bytecode enhancer already removes
 * {@code final} from mapped fields and from fields declared with the {@code transient} keyword -- that
 * happens even with {@code quarkus.hibernate-accessor.strategy=reflection}, where no accessor is generated
 * at all -- so those fields say nothing about what this extension registers.
 */
public class TransientFieldModifiersTest {

    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(MyEntity.class))
            .overrideConfigKey("quarkus.hibernate-accessor.strategy", "generated");

    @Inject
    EntityManager entityManager;

    @Test
    void transientAnnotatedFinalFieldKeepsFinal() throws NoSuchFieldException {
        assertThat(Modifier.isFinal(MyEntity.class.getDeclaredField("annotatedTransient").getModifiers()))
                .as("@Transient field keeps final")
                .isTrue();
    }

    @Test
    void mappedStateStillRoundTrips() {
        QuarkusTransaction.requiringNew().run(() -> {
            MyEntity entity = new MyEntity();
            entity.id = 1L;
            entity.name = "initial";
            entity.tags.add("a");
            entityManager.persist(entity);
        });
        QuarkusTransaction.requiringNew().run(() -> {
            MyEntity loaded = entityManager.find(MyEntity.class, 1L);
            assertThat(loaded.name).isEqualTo("initial");
            assertThat(loaded.tags).containsExactly("a");
            assertThat(loaded.annotatedTransient).isEmpty();
            assertThat(loaded.keywordTransient).isEmpty();
        });
    }

    @Entity
    public static class MyEntity {
        @Id
        public Long id;
        public String name;

        @ElementCollection
        public final List<String> tags = new ArrayList<>();

        @Transient
        public final List<String> annotatedTransient = new ArrayList<>();

        public final transient List<String> keywordTransient = new ArrayList<>();
    }
}
