package io.quarkus.it.amazon.lambda;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

    @Test
    public void testSimpleLambdaSuccess() throws Exception {
        InputObject in = new InputObject();
        in.setGreeting("Hello");
        in.setName("Stu");
        OutputObject out = LambdaClient.invoke(OutputObject.class, in);
        Assertions.assertEquals("Hello Stu", out.getResult());
        Assertions.assertTrue(out.getRequestId().matches("aws-request-\\d"), "Expected requestId as 'aws-request-<number>'");
    }

    @Test
    public void testSimpleLambdaFailure() throws Exception {
        InputObject in = new InputObject();
        in.setGreeting("Hello");
        in.setName("Stuart");
        try {
            OutputObject out = LambdaClient.invoke(OutputObject.class, in);
            out.getResult();
            Assertions.fail();
        } catch (Exception e) {
            Assertions.assertEquals(ProcessingService.CAN_ONLY_GREET_NICKNAMES, e.getMessage());
            //Assertions.assertEquals(IllegalArgumentException.class.getName(), e.getType());
        }

    }
}
