package org.jboss.resteasy.reactive.server.core.parameters.converters;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;

public class ParamConverterTests {
	
	private static final String V1_TESTENDPOINT_GREET = "/v1/testendpoint/greet";
	private static final int OK_200 = 200;
	private static final int TEST_NUMBER = 22;
	
	@Inject
	ParamConverterTestEndpointRest endpoint;
	
	@Test
	void useCustomParamConverter_SendParameter() {
		RequestSpecification requestSpecification = given().queryParam("number", TEST_NUMBER);
		
		//@formatter:off
		Response response = requestSpecification
                .when().get(V1_TESTENDPOINT_GREET)
                .then().extract().response();
        //@formatter:on
		
		assertThat(response.getStatusCode()).isEqualTo(OK_200);
		assertThat(response.getBody().asString()).isEqualTo(String.format(ParamConverterTestEndpointRest.RESPONSE_FORMAT, TEST_NUMBER));
	}
	
	@Test
	void useCumstomParamConverter_DontSendParameter() {
		//@formatter:off
		Response response = given()
                .when().get(V1_TESTENDPOINT_GREET)
                .then().extract().response();
        //@formatter:on
		
		assertThat(response.getStatusCode()).isEqualTo(OK_200);
		assertThat(response.getBody().asString()).isEqualTo(ParamConverterTestEndpointRest.RESPONSE_NO_PARAM);
	}
	
	@Test
	void useCumstomParamConverter_SendEmptyParameter() {
		RequestSpecification requestSpecification = given().queryParam("number", "");
		
		//@formatter:off
		Response response = requestSpecification
                .when().get(V1_TESTENDPOINT_GREET)
                .then().extract().response();
        //@formatter:on
		
		assertThat(response.getStatusCode()).isEqualTo(OK_200);
		assertThat(response.getBody().asString()).isEqualTo(ParamConverterTestEndpointRest.RESPONSE_EMPTY_PARAM);
	}

}
