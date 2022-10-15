package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
public class RegisterForReflectionITCase {

    private static final String BASE_PKG = "io.quarkus.it.rest";
    private static final String ENDPOINT = "/reflection/simpleClassName";

    @Test
    public void testSelfWithoutNested() {
        final String resourceA = BASE_PKG + ".ResourceA";

        assertRegistration("ResourceA", resourceA);
        assertRegistration("FAILED", resourceA + "$InnerClassOfA");
        assertRegistration("FAILED", resourceA + "$StaticClassOfA");
        assertRegistration("FAILED", resourceA + "$InterfaceOfA");
    }

    @Test
    public void testSelfWithNested() {
        final String resourceB = BASE_PKG + ".ResourceB";

        assertRegistration("ResourceB", resourceB);
        assertRegistration("InnerClassOfB", resourceB + "$InnerClassOfB");
        assertRegistration("StaticClassOfB", resourceB + "$StaticClassOfB");
        assertRegistration("InterfaceOfB", resourceB + "$InterfaceOfB");
        assertRegistration("InnerInnerOfB", resourceB + "$InnerClassOfB$InnerInnerOfB");
    }

    @Test
    public void testTargetWithNestedPost22_1() {
        final String resourceC = BASE_PKG + ".ResourceC";

        // Starting with GraalVM 22.1 ResourceC implicitly gets registered by GraalVM
        // (see https://github.com/oracle/graal/pull/4414)
        assertRegistration("ResourceC", resourceC);
        assertRegistration("InaccessibleClassOfC", resourceC + "$InaccessibleClassOfC");
        assertRegistration("OtherInaccessibleClassOfC", resourceC + "$InaccessibleClassOfC$OtherInaccessibleClassOfC");
    }

    @Test
    public void testTargetWithoutNested() {
        final String resourceD = BASE_PKG + ".ResourceD";

        assertRegistration("FAILED", resourceD);
        assertRegistration("StaticClassOfD", resourceD + "$StaticClassOfD");
        assertRegistration("FAILED", resourceD + "$StaticClassOfD$OtherAccessibleClassOfD");
    }

    @Test
    public void testLambdaCapturing() {
        final String resourceLambda = BASE_PKG + ".ResourceLambda";

        // Starting with GraalVM 22.1 support Lambda functions serialization
        // (see https://github.com/oracle/graal/issues/3756)
        RestAssured.given().when().get("/reflection/lambda").then().body(startsWith("Comparator$$Lambda$"));
    }

    private void assertRegistration(String expected, String queryParam) {
        RestAssured.given().queryParam("className", queryParam).when().get(ENDPOINT).then().body(is(expected));
    }
}
