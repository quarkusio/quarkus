package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class NonStaticScheduledAbstractClassTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(IllegalStateException.class)
            .withApplicationRoot(root -> root
                    .addClasses(AbstractClassWitchScheduledMethod.class));

    @Test
    public void test() {
        fail();
    }

    static abstract class AbstractClassWitchScheduledMethod {

        @Scheduled(every = "1s")
        void everySecond() {
        }

    }

}
