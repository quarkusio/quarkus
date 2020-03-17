package io.quarkus.it.amazon.lambda;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class AmazonLambdaSimpleTestCase {

    @Test
    public void testSimpleLambdaSuccess() throws Exception {
        String out = LambdaClient.invoke(String.class, "lowercase");
        Assertions.assertEquals("LOWERCASE", out);
    }
}
