package io.quarkus.hibernate.orm.softdelete;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that entities annotated with {@code @SoftDelete} work correctly.
 * <p>
 * This exercises the {@code Stateful.getStateManagement()} code path which
 * reflectively accesses the {@code INSTANCE} field on {@code StateManagement}
 * implementations via {@code getDeclaredField("INSTANCE")}. In native mode,
 * these fields must be registered for reflection.
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/54777">GitHub issue #54777</a>
 */
public class SoftDeleteTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(SoftDeleteEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    Session session;

    @Test
    @Transactional
    public void testSoftDeleteEntityPersistAndFind() {
        SoftDeleteEntity entity = new SoftDeleteEntity("test");
        session.persist(entity);
        session.flush();

        SoftDeleteEntity found = session.find(SoftDeleteEntity.class, entity.getId());
        assertThat(found).isNotNull();
        assertThat(found.getName()).isEqualTo("test");
    }

    @Test
    @Transactional
    public void testSoftDeleteEntityRemove() {
        SoftDeleteEntity entity = new SoftDeleteEntity("to-delete");
        session.persist(entity);
        session.flush();

        Long id = entity.getId();
        session.remove(entity);
        session.flush();

        SoftDeleteEntity found = session.find(SoftDeleteEntity.class, id);
        assertThat(found).isNull();
    }
}
