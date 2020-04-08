package io.quarkus.funqy.test;

import static io.quarkus.funqy.test.GreetingFunctions.ERR_MSG;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.amazon.lambda.test.LambdaException;

public abstract class GreetTestBase {
    @Test
    public void testGreet() {
        Greeting expectedGreeting = new Greeting("Matej", "Hello Matej!");
        Identity identity = new Identity();
        identity.setName("Matej");
        Greeting actualGreeting = LambdaClient.invoke(Greeting.class, identity);

        Assertions.assertEquals(expectedGreeting, actualGreeting);
    }

    @Test
    public void testGreetNPE() {
        try {
            LambdaClient.invoke(Greeting.class, null);
        } catch (LambdaException e) {
            Assertions.assertTrue(e.getMessage().contains(ERR_MSG));
        }
    }
}
