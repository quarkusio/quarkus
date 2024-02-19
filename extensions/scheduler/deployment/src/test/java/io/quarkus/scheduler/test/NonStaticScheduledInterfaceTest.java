package io.quarkus.scheduler.test;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.test.QuarkusUnitTest;

public class NonStaticScheduledInterfaceTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .setExpectedException(IllegalStateException.class)
            .withApplicationRoot(root -> root
                    .addClasses(InterfaceWitchScheduledMethod.class));

    @Test
    public void test() {
        fail();
    }

    interface InterfaceWitchScheduledMethod {

        @Scheduled(every = "1s")
        void everySecond();

    }

}
