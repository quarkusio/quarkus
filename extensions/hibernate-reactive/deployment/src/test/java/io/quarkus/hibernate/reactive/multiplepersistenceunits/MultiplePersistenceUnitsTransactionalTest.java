package io.quarkus.hibernate.reactive.multiplepersistenceunits;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.hibernate.reactive.mutiny.Mutiny;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.reactive.multiplepersistenceunits.model.config.inventory.Plane;
import io.quarkus.hibernate.reactive.multiplepersistenceunits.model.config.user.User;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class MultiplePersistenceUnitsTransactionalTest {

    @RegisterExtension
    static final QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(User.class, Plane.class, TransactionalWork.class, NestedWork.class))
            .withConfigurationResource("application-multiple-persistence-units-with-default.properties");

    @Inject
    TransactionalWork work;

    @Test
    @RunOnVertxContext
    public void independentTransactionsCanUseDifferentPersistenceUnits(UniAsserter asserter) {
        asserter.assertThat(() -> work.persistUser("separate-user")
                .chain(() -> work.userName("separate-user")), name -> assertThat(name).isEqualTo("separate-user"));
        asserter.assertThat(() -> work.persistPlane("separate-plane")
                .chain(() -> work.planeName("separate-plane")), name -> assertThat(name).isEqualTo("separate-plane"));
    }

    @Test
    @RunOnVertxContext
    public void nestedRequiredCanUseTheSamePersistenceUnit(UniAsserter asserter) {
        asserter.assertThat(work::sameUnitThroughNestedMethod, result -> assertThat(result).isEqualTo(1));
    }

    @Test
    @RunOnVertxContext
    public void statefulSessionsCannotSwitchPersistenceUnits(UniAsserter asserter) {
        assertDifferentUnitRejected(asserter, work::differentStatefulUnits);
    }

    @Test
    @RunOnVertxContext
    public void statefulSessionsCannotSwitchPersistenceUnitsInReverseOrder(UniAsserter asserter) {
        assertDifferentUnitRejected(asserter, work::differentStatefulUnitsInReverseOrder);
    }

    @Test
    @RunOnVertxContext
    public void statefulThenStatelessCannotSwitchPersistenceUnits(UniAsserter asserter) {
        assertDifferentUnitRejected(asserter, work::statefulThenStateless);
    }

    @Test
    @RunOnVertxContext
    public void statelessThenStatefulCannotSwitchPersistenceUnits(UniAsserter asserter) {
        assertDifferentUnitRejected(asserter, work::statelessThenStateful);
    }

    @Test
    @RunOnVertxContext
    public void statelessSessionsCannotSwitchPersistenceUnits(UniAsserter asserter) {
        assertDifferentUnitRejected(asserter, work::differentStatelessUnits);
    }

    @Test
    @RunOnVertxContext
    public void nestedRequiredCannotSwitchPersistenceUnits(UniAsserter asserter) {
        assertDifferentUnitRejected(asserter, work::differentUnitThroughNestedMethod);
    }

    @Test
    @RunOnVertxContext
    public void rejectedSecondUnitRollsBackAndContextCanBeReused(UniAsserter asserter) {
        assertDifferentUnitRejected(asserter, () -> work.persistThenSwitchUnit("must-rollback"));
        asserter.assertThat(() -> work.countUsers("must-rollback"), count -> assertThat(count).isZero());
        // Reusing the same Vert.x context with the other unit must work after rollback.
        asserter.assertThat(work::inventoryQuery, result -> assertThat(result).isEqualTo(1));
        asserter.assertThat(work::usersQuery, result -> assertThat(result).isEqualTo(1));
    }

    private static <T> void assertDifferentUnitRejected(UniAsserter asserter, Supplier<Uni<T>> action) {
        asserter.assertFailedWith(action, failure -> assertThat(failure)
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("same reactive transaction")
                .hasMessageContaining("inventory"));
    }

    @ApplicationScoped
    public static class TransactionalWork {
        @Inject
        Mutiny.Session users;

        @Inject
        @PersistenceUnit("inventory")
        Mutiny.Session inventory;

        @Inject
        Mutiny.StatelessSession statelessUsers;

        @Inject
        @PersistenceUnit("inventory")
        Mutiny.StatelessSession statelessInventory;

        @Inject
        NestedWork nested;

        @Transactional
        public Uni<Void> persistUser(String name) {
            return users.persist(new User(name));
        }

        @Transactional
        public Uni<Void> persistPlane(String name) {
            return inventory.persist(new Plane(name));
        }

        @Transactional
        public Uni<String> userName(String name) {
            return users.createSelectionQuery("select u.name from User u where u.name = :name", String.class)
                    .setParameter("name", name).getSingleResult();
        }

        @Transactional
        public Uni<String> planeName(String name) {
            return inventory.createSelectionQuery("select p.name from Plane p where p.name = :name", String.class)
                    .setParameter("name", name).getSingleResult();
        }

        @Transactional
        public Uni<Integer> usersQuery() {
            return users.createSelectionQuery("select 1", Integer.class).getSingleResult();
        }

        @Transactional
        public Uni<Integer> inventoryQuery() {
            return inventory.createSelectionQuery("select 1", Integer.class).getSingleResult();
        }

        @Transactional
        public Uni<Integer> sameUnitThroughNestedMethod() {
            return users.createSelectionQuery("select 1", Integer.class).getSingleResult()
                    .chain(() -> nested.usersQuery());
        }

        @Transactional
        public Uni<Integer> differentUnitThroughNestedMethod() {
            return users.createSelectionQuery("select 1", Integer.class).getSingleResult()
                    .chain(() -> nested.inventoryQuery());
        }

        @Transactional
        public Uni<Integer> differentStatefulUnits() {
            return users.createSelectionQuery("select 1", Integer.class).getSingleResult()
                    .chain(() -> inventory.createSelectionQuery("select 1", Integer.class).getSingleResult());
        }

        @Transactional
        public Uni<Integer> differentStatefulUnitsInReverseOrder() {
            return inventory.createSelectionQuery("select 1", Integer.class).getSingleResult()
                    .chain(() -> users.createSelectionQuery("select 1", Integer.class).getSingleResult());
        }

        @Transactional
        public Uni<Integer> statefulThenStateless() {
            return users.createSelectionQuery("select 1", Integer.class).getSingleResult()
                    .chain(() -> statelessInventory.createSelectionQuery("select 1", Integer.class).getSingleResult());
        }

        @Transactional
        public Uni<Integer> statelessThenStateful() {
            return statelessUsers.createSelectionQuery("select 1", Integer.class).getSingleResult()
                    .chain(() -> inventory.createSelectionQuery("select 1", Integer.class).getSingleResult());
        }

        @Transactional
        public Uni<Integer> differentStatelessUnits() {
            return statelessUsers.createSelectionQuery("select 1", Integer.class).getSingleResult()
                    .chain(() -> statelessInventory.createSelectionQuery("select 1", Integer.class).getSingleResult());
        }

        @Transactional
        public Uni<Void> persistThenSwitchUnit(String name) {
            return users.persist(new User(name)).call(users::flush)
                    .chain(() -> inventory.createSelectionQuery("select 1", Integer.class).getSingleResult())
                    .replaceWithVoid();
        }

        @Transactional
        public Uni<Long> countUsers(String name) {
            return users.createSelectionQuery("select count(u) from User u where u.name = :name", Long.class)
                    .setParameter("name", name).getSingleResult();
        }
    }

    @ApplicationScoped
    public static class NestedWork {
        @Inject
        Mutiny.Session users;

        @Inject
        @PersistenceUnit("inventory")
        Mutiny.Session inventory;

        @Transactional
        public Uni<Integer> usersQuery() {
            return users.createSelectionQuery("select 1", Integer.class).getSingleResult();
        }

        @Transactional
        public Uni<Integer> inventoryQuery() {
            return inventory.createSelectionQuery("select 1", Integer.class).getSingleResult();
        }
    }
}
