package io.quarkus.it;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.amazon.lambda.deployment.LambdaUtil;

public class UtilTest {

    @Test
    public void testStringUtil() throws Exception {
        Assertions.assertEquals(LambdaUtil.artifactToLambda("foo.bar-1.0-SNAPSHOT"), "FooBar");
        Assertions.assertEquals(LambdaUtil.artifactToLambda("foo..bar--1..0-SNAPSHOT"), "FooBar");
        Assertions.assertEquals(LambdaUtil.artifactToLambda("lambdaxray-1.0-SNAPSHOT"), "Lambdaxray");
        Assertions.assertEquals(LambdaUtil.artifactToLambda("lambdaXray-1.0-SNAPSHOT"), "Lambdaxray");
        Assertions.assertEquals(LambdaUtil.artifactToLambda("quarkus-1.0-rulez"), "QuarkusRulez");
    }
}
