package io.quarkus.hibernate.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.reactive.mutiny.Mutiny;

import io.quarkus.test.vertx.UniAsserter;

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

    public static <T> void testSimplePersistRetrieveUpdateDelete(UniAsserter asserter, Mutiny.SessionFactory sessionFactory,
            Class<T> entityType, Function<String, T> constructor, Function<T, Long> idGetter,
            BiConsumer<T, String> setter, Function<T, String> getter) {
        String initialName = "someName";
        T persistedEntity = constructor.apply(initialName);
        setter.accept(persistedEntity, initialName);
        asserter.assertThat(() -> sessionFactory.withTransaction(session -> session.persist(persistedEntity)
                .call(session::flush)
                .invoke(session::clear)
                .chain(() -> session.find(entityType, idGetter.apply(persistedEntity)))),
                retrievedEntity -> assertThat(retrievedEntity)
                        .extracting(getter)
                        .isEqualTo(initialName));

        // Test updates in order to check dirty properties are correctly detected.
        // This is important for XML mapping in particular since Hibernate ORM's bytecode enhancement ignores XML mapping.
        String updatedName = "someOtherName";
        asserter.assertThat(
                () -> sessionFactory.withTransaction(session -> session.find(entityType, idGetter.apply(persistedEntity))
                        .invoke(entity -> setter.accept(entity, updatedName))
                        .call(session::flush)
                        .invoke(session::clear)
                        .chain(() -> session.find(entityType, idGetter.apply(persistedEntity)))),
                retrievedEntity -> assertThat(retrievedEntity)
                        .extracting(getter)
                        .isEqualTo(updatedName));

        // Test updates in order to check dirty properties are correctly detected.
        // This is important for XML mapping in particular since Hibernate ORM's bytecode enhancement ignores XML mapping.
        asserter.assertThat(
                () -> sessionFactory.withTransaction(session -> session.find(entityType, idGetter.apply(persistedEntity))
                        .call(session::remove)
                        .call(session::flush)
                        .invoke(session::clear)
                        .chain(() -> session.find(entityType, idGetter.apply(persistedEntity)))),
                retrievedEntity -> assertThat(retrievedEntity).isNull());
    }

}
