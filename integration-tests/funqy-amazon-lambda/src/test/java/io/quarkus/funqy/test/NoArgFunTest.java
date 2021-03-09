package io.quarkus.funqy.test;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.quarkus.amazon.lambda.test.LambdaClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@ExtendWith(UseNoArgFunExtension.class)
public class NoArgFunTest {

    @Test
    public void testNoArgFun() throws Exception {
        LambdaClient.invoke(String.class, null, Duration.ofSeconds(5));
    }
}
