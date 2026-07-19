package io.quarkus.vertx.http.cors;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

class CORSAccessControlWildcardTestCase
{

	@RegisterExtension
	static QuarkusUnitTest runner = new QuarkusUnitTest()
		.withApplicationRoot((jar) -> jar
			.addClasses(BeanRegisteringRoute.class)
			.addAsResource("conf/cors-access-control-wildcard.properties", "application.properties"));

	@Test
	@DisplayName("Checks that setting the config of the Access-Control-* header to wildcard will end up in at the client.")
	void corsControlExposeHeaderCanBeWildcards()
	{

		given().header("Origin", "http://custom.origin.quarkus")
			.when()
			.get("/test").then()
			.statusCode(200)
			.header("Access-Control-Expose-Headers", "*");
	}
	@Test
	@DisplayName("Checks that setting the config of the Access-Control-* header to wildcard will end up in at the client.")
	void corsAccessControlHeaderCanBeWildcards()
	{

		given().header("Origin", "http://custom.origin.quarkus")
			.when()
			.options("/test").then()
			.statusCode(200)
			.header("Access-Control-Allow-Origin", "http://custom.origin.quarkus")
			.header("Access-Control-Allow-Methods", "*")
			.header("Access-Control-Allow-Headers", "*");
	}

}
