package io.quarkus.data.hibernate.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

/**
 * Tests {@code findById} on managed (stateful) and stateless repositories, blocking and reactive,
 * for both an existing and a missing entity.
 */
public class FindByIdTest {

    private static final long MISSING_ID = 123_456_789L;

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(FindByIdEntity.class, FindByIdEntity_.class, _FindByIdEntity.class,
                            _FindByIdEntity._ManagedBlockingRepo.class,
                            _FindByIdEntity._StatelessBlockingRepo.class,
                            _FindByIdEntity._ManagedReactiveRepo.class,
                            _FindByIdEntity._StatelessReactiveRepo.class));

    @Inject
    FindByIdEntity.ManagedBlockingRepo managedBlocking;

    @Inject
    FindByIdEntity.StatelessBlockingRepo statelessBlocking;

    @Inject
    FindByIdEntity.ManagedReactiveRepo managedReactive;

    @Inject
    FindByIdEntity.StatelessReactiveRepo statelessReactive;

    @Transactional
    Long createEntityBlocking() {
        managedBlocking.deleteAll();
        FindByIdEntity entity = new FindByIdEntity();
        entity.foo = "found";
        entity.persist();
        return entity.id;
    }

    @Transactional
    Uni<Long> createEntityReactive() {
        return managedReactive.deleteAll().flatMap(v -> {
            FindByIdEntity entity = new FindByIdEntity();
            entity.foo = "found";
            return managedReactive.persist(entity).map(x -> entity.id);
        });
    }

    // Managed, blocking

    @Transactional
    void managedBlockingFound(Long id) {
        FindByIdEntity entity = managedBlocking.findById(id);
        assertThat(entity).isNotNull();
        assertThat(entity.id).isEqualTo(id);
        assertThat(entity.foo).isEqualTo("found");
    }

    @Transactional
    void managedBlockingOptionalFound(Long id) {
        Optional<FindByIdEntity> entity = managedBlocking.findByIdOptional(id);
        assertThat(entity).isPresent();
        assertThat(entity.get().id).isEqualTo(id);
        assertThat(entity.get().foo).isEqualTo("found");

        Optional<FindByIdEntity> locked = managedBlocking.findByIdOptional(id, LockModeType.PESSIMISTIC_READ);
        assertThat(locked).isPresent();
        assertThat(locked.get().id).isEqualTo(id);
    }

    @Transactional
    void managedBlockingOptionalNotFound() {
        assertThat(managedBlocking.findByIdOptional(MISSING_ID)).isEmpty();
        assertThat(managedBlocking.findByIdOptional(MISSING_ID, LockModeType.PESSIMISTIC_READ)).isEmpty();
    }

    @Transactional
    void managedBlockingNotFound() {
        assertThat(managedBlocking.findById(MISSING_ID)).isNull();
    }

    @Test
    void testManagedBlocking() {
        Long id = createEntityBlocking();
        managedBlockingFound(id);
        managedBlockingNotFound();
        managedBlockingOptionalFound(id);
        managedBlockingOptionalNotFound();
    }

    // Stateless, blocking

    @Transactional
    void statelessBlockingFound(Long id) {
        FindByIdEntity entity = statelessBlocking.findById(id);
        assertThat(entity).isNotNull();
        assertThat(entity.id).isEqualTo(id);
        assertThat(entity.foo).isEqualTo("found");

        FindByIdEntity locked = statelessBlocking.findById(id, LockModeType.PESSIMISTIC_READ);
        assertThat(locked).isNotNull();
        assertThat(locked.id).isEqualTo(id);
    }

    @Transactional
    void statelessBlockingOptionalFound(Long id) {
        Optional<FindByIdEntity> entity = statelessBlocking.findByIdOptional(id);
        assertThat(entity).isPresent();
        assertThat(entity.get().id).isEqualTo(id);
        assertThat(entity.get().foo).isEqualTo("found");

        Optional<FindByIdEntity> locked = statelessBlocking.findByIdOptional(id, LockModeType.PESSIMISTIC_READ);
        assertThat(locked).isPresent();
        assertThat(locked.get().id).isEqualTo(id);
    }

    @Transactional
    void statelessBlockingOptionalNotFound() {
        assertThat(statelessBlocking.findByIdOptional(MISSING_ID)).isEmpty();
        assertThat(statelessBlocking.findByIdOptional(MISSING_ID, LockModeType.PESSIMISTIC_READ)).isEmpty();
    }

    @Transactional
    void statelessBlockingNotFound() {
        assertThat(statelessBlocking.findById(MISSING_ID)).isNull();
        assertThat(statelessBlocking.findById(MISSING_ID, LockModeType.PESSIMISTIC_READ)).isNull();
    }

    @Test
    void testStatelessBlocking() {
        Long id = createEntityBlocking();
        statelessBlockingFound(id);
        statelessBlockingNotFound();
        statelessBlockingOptionalFound(id);
        statelessBlockingOptionalNotFound();
    }

    // Managed, reactive

    @Transactional
    Uni<Void> managedReactiveFound(Long id) {
        return managedReactive.findById(id).map(entity -> {
            assertThat(entity).isNotNull();
            assertThat(entity.id).isEqualTo(id);
            assertThat(entity.foo).isEqualTo("found");
            return null;
        });
    }

    @Transactional
    Uni<Void> managedReactiveNotFound() {
        return managedReactive.findById(MISSING_ID).map(entity -> {
            assertThat(entity).isNull();
            return null;
        });
    }

    @RunOnVertxContext
    @Test
    void testManagedReactive(UniAsserter asserter) {
        asserter.assertThat(() -> createEntityReactive(), id -> asserter.putData("id", id));
        asserter.execute(() -> managedReactiveFound((Long) asserter.getData("id")));
        asserter.execute(() -> managedReactiveNotFound());
    }

    // Stateless, reactive

    @Transactional
    Uni<Void> statelessReactiveFound(Long id) {
        return statelessReactive.findById(id).map(entity -> {
            assertThat(entity).isNotNull();
            assertThat(entity.id).isEqualTo(id);
            assertThat(entity.foo).isEqualTo("found");
            return null;
        }).chain(() -> statelessReactive.findById(id, LockModeType.PESSIMISTIC_READ)).map(locked -> {
            assertThat(locked).isNotNull();
            assertThat(locked.id).isEqualTo(id);
            return null;
        });
    }

    @Transactional
    Uni<Void> statelessReactiveNotFound() {
        return statelessReactive.findById(MISSING_ID).map(entity -> {
            assertThat(entity).isNull();
            return null;
        }).chain(() -> statelessReactive.findById(MISSING_ID, LockModeType.PESSIMISTIC_READ)).map(entity -> {
            assertThat(entity).isNull();
            return null;
        });
    }

    @RunOnVertxContext
    @Test
    void testStatelessReactive(UniAsserter asserter) {
        asserter.assertThat(() -> createEntityReactive(), id -> asserter.putData("id", id));
        asserter.execute(() -> statelessReactiveFound((Long) asserter.getData("id")));
        asserter.execute(() -> statelessReactiveNotFound());
    }
}
