package io.quarkus.it.main;

import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class RegisterForReflectionTestCase {
    private static final String BASE_PKG = "io.quarkus.it.rest";
    private static final String ENDPOINT = "/reflection/simpleClassName";

    @Test
    public void testSelfWithoutNested() {
        final String resourceA = BASE_PKG + ".ResourceA";

        assertRegistration("ResourceA", resourceA);
        assertRegistration("InnerClassOfA", resourceA + "$InnerClassOfA");
        assertRegistration("StaticClassOfA", resourceA + "$StaticClassOfA");
        assertRegistration("InterfaceOfA", resourceA + "$InterfaceOfA");
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
    public void testTargetWithNested() {
        final String resourceC = BASE_PKG + ".ResourceC";

        assertRegistration("ResourceC", resourceC);
        assertRegistration("InaccessibleClassOfC", resourceC + "$InaccessibleClassOfC");
        assertRegistration("OtherInaccessibleClassOfC", resourceC + "$InaccessibleClassOfC$OtherInaccessibleClassOfC");
    }

    @Test
    public void testTargetWithoutNested() {
        final String resourceD = BASE_PKG + ".ResourceD";

        assertRegistration("ResourceD", resourceD);
        assertRegistration("StaticClassOfD", resourceD + "$StaticClassOfD");
        assertRegistration("OtherAccessibleClassOfD", resourceD + "$StaticClassOfD$OtherAccessibleClassOfD");
    }

    private void assertRegistration(String expected, String queryParam) {
        RestAssured.given().queryParam("className", queryParam).when().get(ENDPOINT).then().body(is(expected));
    }

}
