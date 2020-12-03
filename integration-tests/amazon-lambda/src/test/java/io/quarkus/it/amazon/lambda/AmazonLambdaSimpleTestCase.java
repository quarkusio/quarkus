package io.quarkus.it.amazon.lambda;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

    @Test
    public void testSimpleLambdaSuccess() throws Exception {
        resetCounter();
        InputObject in = new InputObject();
        in.setGreeting("Hello");
        in.setName("Stu");
        OutputObject out = LambdaClient.invoke(OutputObject.class, in);
        assertEquals("Hello Stu", out.getResult());
        assertCounter(1);
        assertTrue(out.getRequestId().matches("aws-request-\\d"), "Expected requestId as 'aws-request-<number>'");
    }

    @Test
    public void testSimpleLambdaFailure() throws Exception {
        resetCounter();
        InputObject in = new InputObject();
        in.setGreeting("Hello");
        in.setName("Stuart");
        try {
            OutputObject out = LambdaClient.invoke(OutputObject.class, in);
            out.getResult();
            fail();
        } catch (Exception e) {
            assertEquals(ProcessingService.CAN_ONLY_GREET_NICKNAMES, e.getMessage());
            //Assertions.assertEquals(IllegalArgumentException.class.getName(), e.getType());
        }
        assertCounter(1);
    }

    private void resetCounter() {
        if (!isNativeImageTest()) {
            CounterInterceptor.COUNTER.set(0);
        }
    }

    private void assertCounter(int expected) {
        if (!isNativeImageTest()) {
            assertEquals(expected, CounterInterceptor.COUNTER.get());
        }
    }

    private boolean isNativeImageTest() {
        return System.getProperty("native.image.path") != null;
    }
}
