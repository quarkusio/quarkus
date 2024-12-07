package io.quarkus.test.security.webauthn;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;

import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.vertx.core.json.JsonObject;

public class WebAuthnEndpointHelper {
    public static String obtainRegistrationChallenge(String userName, Filter cookieFilter) {
        JsonObject registerJson = new JsonObject()
                .put("name", userName);
        ExtractableResponse<Response> response = RestAssured
                .given().body(registerJson.encode())
                .contentType(ContentType.JSON)
                .filter(cookieFilter)
                .log().ifValidationFails()
                .post("/q/webauthn/register-options-challenge")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .cookie(getChallengeCookie(), Matchers.notNullValue())
                .extract();
        // assert stuff
        JsonObject responseJson = new JsonObject(response.asString());
        String challenge = responseJson.getString("challenge");
        Assertions.assertNotNull(challenge);
        return challenge;
    }

    public static void invokeLogin(JsonObject login, Filter cookieFilter) {
        RestAssured
                .given().body(login.encode())
                .filter(cookieFilter)
                .contentType(ContentType.JSON)
                .log().ifValidationFails()
                .post("/q/webauthn/login")
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .cookie(getChallengeCookie(), Matchers.is(""))
                .cookie(getMainCookie(), Matchers.notNullValue());
    }

    public static void invokeRegistration(String username, JsonObject registration, Filter cookieFilter) {
        RestAssured
                .given().body(registration.encode())
                .filter(cookieFilter)
                .contentType(ContentType.JSON)
                .log().ifValidationFails()
                .queryParam("username", username)
                .post("/q/webauthn/register")
                .then()
                .log().ifValidationFails()
                .statusCode(204)
                .cookie(getChallengeCookie(), Matchers.is(""))
                .cookie(getMainCookie(), Matchers.notNullValue());
    }

    public static String obtainLoginChallenge(String userName, Filter cookieFilter) {
        JsonObject loginJson = new JsonObject()
                .put("name", userName);
        ExtractableResponse<Response> response = RestAssured
                .given().body(loginJson.encode())
                .contentType(ContentType.JSON)
                .filter(cookieFilter)
                .log().ifValidationFails()
                .post("/q/webauthn/login-options-challenge")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .cookie(getChallengeCookie(), Matchers.notNullValue())
                .extract();
        // assert stuff
        JsonObject responseJson = new JsonObject(response.asString());
        String challenge = responseJson.getString("challenge");
        Assertions.assertNotNull(challenge);
        return challenge;
    }

    public static void addWebAuthnRegistrationFormParameters(RequestSpecification request, JsonObject registration) {
        request
                .formParam("webAuthnId", registration.getString("id"))
                .formParam("webAuthnRawId", registration.getString("rawId"))
                .formParam("webAuthnResponseAttestationObject",
                        registration.getJsonObject("response").getString("attestationObject"))
                .formParam("webAuthnResponseClientDataJSON", registration.getJsonObject("response").getString("clientDataJSON"))
                .formParam("webAuthnType", registration.getString("type"));
    }

    public static void addWebAuthnLoginFormParameters(RequestSpecification request, JsonObject login) {
        request
                .formParam("webAuthnId", login.getString("id"))
                .formParam("webAuthnRawId", login.getString("rawId"))
                .formParam("webAuthnResponseAuthenticatorData", login.getJsonObject("response").getString("authenticatorData"))
                .formParam("webAuthnResponseSignature", login.getJsonObject("response").getString("signature"))
                .formParam("webAuthnResponseUserHandle", login.getJsonObject("response").getString("userHandle"))
                .formParam("webAuthnResponseClientDataJSON", login.getJsonObject("response").getString("clientDataJSON"))
                .formParam("webAuthnType", login.getString("type"));
    }

    public static void invokeLogout(Filter cookieFilter) {
        RestAssured.given().filter(cookieFilter)
                .redirects().follow(false)
                .when()
                .log().ifValidationFails()
                .get("/q/webauthn/logout")
                .then()
                .log().ifValidationFails()
                .statusCode(302)
                .cookie(getMainCookie(), Matchers.is(""));
    }

    public static String getMainCookie() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue("quarkus.webauthn.cookie-name", String.class).orElse("quarkus-credential");
    }

    public static String getChallengeCookie() {
        Config config = ConfigProvider.getConfig();
        return config.getOptionalValue("quarkus.webauthn.challenge-cookie-name", String.class)
                .orElse("_quarkus_webauthn_challenge");
    }
}
