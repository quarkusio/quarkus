package io.quarkus.narayana.quarkus;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.HeuristicMixedException;
import jakarta.transaction.HeuristicRollbackException;
import jakarta.transaction.NotSupportedException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Test that interposed synchronizations are called in the correct order
 * See {@code AgroalOrderedLastSynchronizationList} for the implementation
 */
public class TSRTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Fruit.class, FruitResource.class)
                    .addAsResource("application-tsr.properties"));

    @Inject
    TransactionSynchronizationRegistry tsr;

    @Inject
    TransactionManager tm;

    @Inject
    Event<String> event;

    private enum SYNCH_TYPES {
        AGROAL,
        HIBERNATE,
        OTHER
    };

    private static final List<String> synchronizationCallbacks = new ArrayList<>();

    @BeforeEach
    public void before() {
        synchronizationCallbacks.clear();
    }

    @Test
    public void test() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        tm.begin();

        RestAssured.given()
                .when()
                .body("{\"name\" : \"Pear\"}")
                .contentType("application/json")
                .post("/fruits")
                .then()
                .statusCode(201);

        // register a synchronization that registers more synchronizations during the beforeCompletion callback
        tsr.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                synchronizationCallbacks.add(SYNCH_TYPES.OTHER.name());

                // Add another synchronization belonging to the same "category".
                // This registration should succeed since it belongs to the same group that's currently being processed.
                // But note that adding one for a group that has already finished should fail (but we cannot test that
                // here since the other groups belong to different packages, ie hibernate and agroal).
                tsr.registerInterposedSynchronization(new NormalSynchronization());
            }

            @Override
            public void afterCompletion(int status) {
            }
        });

        // cause ARC to register a callback for transaction lifecycle events (see ObservingBean), but since ARC
        // uses a session synchronization this should *not* result in an interposed synchronization being registered
        event.fire("commit");

        tm.commit();

        /*
         * Check that the two normal synchronizations added by this test were invoked.
         * The actual list is maintained by {@code AgroalOrderedLastSynchronizationList}
         * and it will also include interposed synchronizations added by hibernate
         * and Agroal as a result of calling the above hibernate query.
         * If you want to verify that the order is correct then run the test under
         * the control of a debugger and look at the order of the list maintained
         * by the AgroalOrderedLastSynchronizationList class.
         */
        Assertions.assertEquals(2, synchronizationCallbacks.size());
        Assertions.assertEquals(SYNCH_TYPES.OTHER.name(), synchronizationCallbacks.get(0));
        Assertions.assertEquals(SYNCH_TYPES.OTHER.name(), synchronizationCallbacks.get(1));
    }

    @Test
    public void testException()
            throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException,
            RollbackException {
        final String MESSAGE = "testException from synchronization";
        final NormalSynchronization normalSynchronization = new NormalSynchronization();

        tm.begin();

        RestAssured.given()
                .when()
                .body("{\"name\" : \"Orange\"}") // use a different fruit from the other tests in this suite
                .contentType("application/json")
                .post("/fruits")
                .then()
                .statusCode(201);

        // register a synchronization that registers more synchronizations during the beforeCompletion callback
        tsr.registerInterposedSynchronization(new Synchronization() {
            @Override
            public void beforeCompletion() {
                synchronizationCallbacks.add(SYNCH_TYPES.OTHER.name());

                // Add another synchronization belonging to the same "category".
                // This registration should succeed since it belongs to the same group that's currently being processed.
                // But note that adding one for a group that has already finished should fail (but we cannot test that
                // here since the other groups belong to different packages, ie hibernate and agroal).
                tsr.registerInterposedSynchronization(normalSynchronization);

                // throw an exception to verify that the other beforeCompletion synchronizations still execute
                throw new RuntimeException(MESSAGE);
            }

            @Override
            public void afterCompletion(int status) {
            }
        });

        try {
            tm.commit();

            Assertions.fail("Expected commit to throw an exception");
        } catch (RollbackException | HeuristicMixedException | HeuristicRollbackException | SecurityException
                | IllegalStateException | SystemException e) {
            Assertions.assertNotNull(e.getCause(), "expected exception cause to be present");
            Assertions.assertTrue(e.getCause().getMessage().endsWith(MESSAGE),
                    "expected a different exception message");

            // the synchronization registered a synchronization (the variable normalSynchronization)
            // just before it threw the exception so now check that it was still called:
            Assertions.assertTrue(normalSynchronization.wasInvoked(),
                    "the synchronization registered before the exception should have ran");

            /*
             * Check that the two normal synchronizations added by this test were invoked.
             * The actual list is maintained by {@code AgroalOrderedLastSynchronizationList}
             * and it will also include interposed synchronizations added by hibernate
             * and Agroal as a result of calling the above hibernate query.
             * If you want to verify that the order is correct then run the test under
             * the control of a debugger and look at the order of the list maintained
             * by the AgroalOrderedLastSynchronizationList class.
             */
            Assertions.assertEquals(2, synchronizationCallbacks.size());
            Assertions.assertEquals(SYNCH_TYPES.OTHER.name(), synchronizationCallbacks.get(0));
            Assertions.assertEquals(SYNCH_TYPES.OTHER.name(), synchronizationCallbacks.get(1));
        }
    }

    @ApplicationScoped
    static class ObservingBean {
        @Inject
        EntityManager entityManager;

        // observing beforeCompletion is what triggered the issue about the need to order Agroal synchronizations last
        public void observeBeforeCompletion(@Observes(during = TransactionPhase.BEFORE_COMPLETION) String payload) {
            final var list = entityManager.createNamedQuery("Fruits.findAll", Fruit.class).getResultList();
            Assertions.assertFalse(list.isEmpty()); // a Pear should have been added
        }
    }

    // define another synchronization to test various things such as verifying that synchronizations can be
    // registered by other synchronizations and that later synchronizations still run even though earlier ones
    // may have thrown exceptions
    private static class NormalSynchronization implements Synchronization {
        private boolean invoked;

        @Override
        public void beforeCompletion() {
            synchronizationCallbacks.add(SYNCH_TYPES.OTHER.name());
            invoked = true;
        }

        @Override
        public void afterCompletion(int status) {
        }

        public boolean wasInvoked() {
            return invoked;
        }
    }
}
