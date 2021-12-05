package io.quarkus.narayana.observers;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that Arc transactional observers work with Narayana-provided Synchronization registry.
 * All observers also make use of Request scoped bean so that we verify that the context is automatically activated.
 */
public class TransactionalObserversTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ObservingBean.class, Actions.class));

    public static String AFTER_SUCCESS = "AFTER_SUCCESS";
    public static String AFTER_COMPLETION = "AFTER_COMPLETION";
    public static String AFTER_FAILURE = "AFTER_FAILURE";
    public static String BEFORE_COMPLETION = "BEFORE_COMPLETION";
    public static String PLAIN = "PLAIN";

    @BeforeEach
    public void before() {
        Actions.clear();
    }

    @Inject
    UserTransaction tx;

    @Inject
    Event<String> event;

    @Test
    public void testTransactionSuccessful() throws Exception {
        tx.begin();
        event.fire("commit");
        Assertions.assertTrue(Actions.getActions().size() == 1);
        Assertions.assertTrue(Actions.contains(TransactionalObserversTest.PLAIN));
        tx.commit();
        List<String> actions = Actions.getActions();
        Assertions.assertTrue(actions.size() == 4);
        Actions.contains(TransactionalObserversTest.AFTER_COMPLETION, TransactionalObserversTest.AFTER_SUCCESS,
                TransactionalObserversTest.BEFORE_COMPLETION);
        Actions.precedes(TransactionalObserversTest.BEFORE_COMPLETION, TransactionalObserversTest.AFTER_COMPLETION,
                TransactionalObserversTest.AFTER_SUCCESS);
    }

    @Test
    public void testTransactionFailed() throws Exception {
        tx.begin();
        event.fire("rollback");
        Assertions.assertTrue(Actions.getActions().size() == 1);
        Assertions.assertTrue(Actions.contains(TransactionalObserversTest.PLAIN));
        tx.rollback();
        Assertions.assertTrue(Actions.getActions().size() == 3);
        Actions.contains(TransactionalObserversTest.AFTER_COMPLETION, TransactionalObserversTest.AFTER_FAILURE);
    }

    @Test
    public void testOutsideTransaction() {
        event.fire("outsideTx");
        Assertions.assertTrue(Actions.getActions().size() == 5);
        Actions.contains(TransactionalObserversTest.AFTER_COMPLETION, TransactionalObserversTest.AFTER_FAILURE,
                TransactionalObserversTest.BEFORE_COMPLETION, TransactionalObserversTest.AFTER_SUCCESS,
                TransactionalObserversTest.PLAIN);
    }

    @ApplicationScoped
    static class ObservingBean {

        public void observeAfterSuccess(@Observes(during = TransactionPhase.AFTER_SUCCESS) String payload, ReqScopedBean bean) {
            Actions.add(TransactionalObserversTest.AFTER_SUCCESS);
            bean.ping();
        }

        public void observeAfterFailure(@Observes(during = TransactionPhase.AFTER_FAILURE) String payload, ReqScopedBean bean) {
            Actions.add(TransactionalObserversTest.AFTER_FAILURE);
            bean.ping();
        }

        public void observeAfterCompletion(@Observes(during = TransactionPhase.AFTER_COMPLETION) String payload,
                ReqScopedBean bean) {
            Actions.add(TransactionalObserversTest.AFTER_COMPLETION);
            bean.ping();
        }

        public void observeBeforeCompletion(@Observes(during = TransactionPhase.BEFORE_COMPLETION) String payload,
                ReqScopedBean bean) {
            Actions.add(TransactionalObserversTest.BEFORE_COMPLETION);
            bean.ping();
        }

        public void classicObserver(@Observes String payload, ReqScopedBean bean) {
            Actions.add(TransactionalObserversTest.PLAIN);
            bean.ping();
        }
    }

    @RequestScoped
    static class ReqScopedBean {
        // just to verify that the context gets activated for OMs
        public void ping() {
        }
    }

    static class Actions {

        private static List<String> actions = new ArrayList<String>();

        public static List<String> getActions() {
            return actions;
        }

        public static void clear() {
            actions.clear();
        }

        public static boolean add(Object o) {
            return actions.add(o.toString());
        }

        public static boolean isSequence(Object... seq) {
            int i = 0;
            return objectsToStrings(seq).equals(actions);
        }

        // true iff obj exists and all otherObjects exist and indexOf(obj) < indexOf(x) for each x from otherObjects
        public static boolean precedes(Object obj, Object... otherObjects) {
            boolean precedes = true;
            int i = 0;
            if (precedes = (Actions.contains(obj) && Actions.contains(otherObjects))) {
                while (i < otherObjects.length && (precedes = precedes
                        && actions.indexOf(obj.toString()) < actions.indexOf(otherObjects[i++].toString())))
                    ;
            }
            return precedes;
        }

        public static boolean startsWith(Object... objects) {
            return actions.subList(0, objects.length).equals(objectsToStrings(objects));
        }

        public static boolean endsWith(Object... objects) {
            return actions.subList(actions.size() - objects.length, actions.size()).equals(objectsToStrings(objects));
        }

        public static boolean contains(Object... objects) {
            return actions.containsAll(objectsToStrings(objects));
        }

        private static List<String> objectsToStrings(final Object... objects) {
            List<String> result = new ArrayList<String>();
            for (Object obj : objects) {
                result.add(obj.toString());
            }
            return result;
        }
    }

}
