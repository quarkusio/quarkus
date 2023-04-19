package org.acme

import static io.restassured.RestAssured.given
import static org.hamcrest.CoreMatchers.is

import org.junit.jupiter.api.Test

import io.quarkus.test.junit.QuarkusTest

@QuarkusTest
class MyDeclarativeRoutesTest {
	@Test
	void testHelloRouteEndpointWithNameParameter() {
		given().when().get("hello-route?name=Quarkus").then().statusCode(200).body(is("Hello Quarkus !!"));
	}

	@Test
	void testHelloRouteEndpointWithoutNameParameter() {
		given().when().get("hello-route").then().statusCode(200).body(is("Hello Reactive Route !!"));
	}

}
