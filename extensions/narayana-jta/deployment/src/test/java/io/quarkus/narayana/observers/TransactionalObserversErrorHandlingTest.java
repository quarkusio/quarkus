package io.quarkus.narayana.observers;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.event.TransactionPhase;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests that when an observer throws an exception, this doesn't crash the application or prevents other observers from
 * being notified.
 */
public class TransactionalObserversErrorHandlingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ObservingBean.class));

    @Inject
    UserTransaction tx;

    @Inject
    Event<String> event;

    @Test
    public void testObserverNotificationWithErrorThrowing() throws Exception {
        ObservingBean.TIMES_NOTIFIED = 0;
        tx.begin();
        event.fire("foo");
        Assertions.assertTrue(ObservingBean.TIMES_NOTIFIED == 0);
        tx.commit();
        Assertions.assertTrue(ObservingBean.TIMES_NOTIFIED == 2);
    }

    @ApplicationScoped
    static class ObservingBean {

        public static int TIMES_NOTIFIED = 0;

        public void observeAfterSuccess(@Observes(during = TransactionPhase.AFTER_SUCCESS) String payload) {
            TIMES_NOTIFIED++;
            throw new IllegalStateException("This is an expected exception within test");
        }

        public void observeAfterSuccess2(@Observes(during = TransactionPhase.AFTER_SUCCESS) String payload) {
            TIMES_NOTIFIED++;
            throw new IllegalStateException("This is an expected exception within test");

        }
    }
}
