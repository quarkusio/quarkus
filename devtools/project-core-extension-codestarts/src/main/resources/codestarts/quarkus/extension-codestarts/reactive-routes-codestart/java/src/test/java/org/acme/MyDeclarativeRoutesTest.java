package org.acme;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class MyDeclarativeRoutesTest {

	@Test
	public void testHelloEndpoint() {
		given().when().get("/hello").then().statusCode(200).body(is("Hello RESTEasy Reactive Route"));
	}

	@Test
	public void testWorldEndpoint() {
		given().when().get("/world").then().statusCode(200).body(is("Hello world !!"));
	}

	@Test
	public void testGreetingEndpointWithNameParameter() {
		given().when().get("/greetings?name=Quarkus").then().statusCode(200).body(is("Hello  Quarkus !!"));
	}

	@Test
	public void testGreetingEndpointWithoutNameParameter() {
		given().when().get("/greetings").then().statusCode(200).body(is("Hello  RESTEasy Reactive Route !!"));
	}

}
