package org.acme.lambda;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LambdaHandlerTest {

    @Test
    public void testSimpleLambdaSuccess() throws Exception {
        Person in = new Person();
        in.setName("Stu");
        String out = LambdaClient.invoke(String.class, in);
        Assertions.assertEquals("Hello Stu", out);
    }

}
