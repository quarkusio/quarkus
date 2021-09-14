package io.quarkus.hibernate.orm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.persistence.EntityManager;

/**
 * Very simple reusable tests that simply check that persistence doesn't seem to explode.
 * <p>
 * Ideally we should rather use an abstract base class and subclass it with an inner class annotated with
 * {@link org.junit.jupiter.api.Nested @Nested} in relevant tests.
 * But unfortunately {@code @Nested} doesn't work with {@link io.quarkus.test.QuarkusUnitTest} at the moment.
 */
public final class SmokeTestUtils {

    private SmokeTestUtils() {
    }

    public static <T> void testSimplePersistRetrieveUpdateDelete(EntityManager entityManager,
            Class<T> entityType, Function<String, T> constructor, Function<T, Long> idGetter,
            BiConsumer<T, String> setter, Function<T, String> getter) {
        String initialName = "someName";
        T persistedEntity = constructor.apply(initialName);
        setter.accept(persistedEntity, initialName);
        entityManager.persist(persistedEntity);
        entityManager.flush();
        entityManager.clear();
        T retrievedEntity = entityManager.find(entityType, idGetter.apply(persistedEntity));
        assertThat(retrievedEntity)
                .extracting(getter)
                .isEqualTo(initialName);

        // Test updates in order to check dirty properties are correctly detected.
        // This is important for XML mapping in particular since Hibernate ORM's bytecode enhancement ignores XML mapping.
        String updatedName = "someOtherName";
        setter.accept(retrievedEntity, updatedName);
        entityManager.flush();
        entityManager.clear();
        retrievedEntity = entityManager.find(entityType, idGetter.apply(persistedEntity));
        assertThat(retrievedEntity)
                .extracting(getter)
                .isEqualTo(updatedName);

        // Test updates in order to check dirty properties are correctly detected.
        // This is important for XML mapping in particular since Hibernate ORM's bytecode enhancement ignores XML mapping.
        entityManager.remove(retrievedEntity);
        entityManager.flush();
        entityManager.clear();
        retrievedEntity = entityManager.find(entityType, idGetter.apply(persistedEntity));
        assertThat(retrievedEntity).isNull();
    }

}
