package org.acme.funqy;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class FunqyTest {
    @Test
    public void testSimpleLambdaSuccess() throws Exception {
        Person friend = new Person("Bill");
        String out = LambdaClient.invoke(String.class, friend);
        Assertions.assertEquals("Hello Bill", out);
    }
}
