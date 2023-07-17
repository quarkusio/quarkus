package io.quarkus.smallrye.graphql.deployment;

import static org.hamcrest.CoreMatchers.containsString;

import jakarta.validation.constraints.Size;

import org.eclipse.microprofile.graphql.GraphQLApi;
import org.eclipse.microprofile.graphql.Query;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;

/**
 * Test for localization of bean validation messages from a GraphQL endpoint,
 * with the language determined by the Accept-Language header passed by a client.
 */
public class GraphQLValidationMessagesLocalizationTest extends AbstractGraphQLTest {

    private static final String ERROR_MESSAGE_GERMAN = "Nachricht ist zu lang";
    private static final String ERROR_MESSAGE_SPANISH = "El mensaje es demasiado largo";
    private static final String ERROR_MESSAGE_ENGLISH = "Message is too long";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(ApiWithValidation.class)
                    .addAsResource(new StringAsset("quarkus.locales=es_ES,en_US,de_DE\n" +
                            "quarkus.default-locale=de_DE"), "application.properties")
                    .addAsResource(new StringAsset("message.too.long=" + ERROR_MESSAGE_ENGLISH),
                            "ValidationMessages.properties")
                    .addAsResource(new StringAsset("message.too.long=" + ERROR_MESSAGE_SPANISH),
                            "ValidationMessages_es.properties")
                    .addAsResource(new StringAsset("message.too.long=" + ERROR_MESSAGE_GERMAN),
                            "ValidationMessages_de.properties")
                    .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml"));

    @GraphQLApi
    public static class ApiWithValidation {

        @Query
        @NonBlocking
        public String echo(@Size(max = 4, message = "{message.too.long}") String input) {
            return input;
        }

        @Query
        @Blocking
        public String echoBlocking(@Size(max = 4, message = "{message.too.long}") String input) {
            return input;
        }
    }

    @Test
    public void testAcceptSpanishBlocking() {
        String query = getPayload("{echoBlocking(input:\"TOO LONG\")}");

        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .header(new Header("Accept-Language", "es"))
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("errors[0].message", containsString(ERROR_MESSAGE_SPANISH));
    }

    @Test
    public void testDefaultLocaleBlocking() {
        String query = getPayload("{echoBlocking(input:\"TOO LONG\")}");

        // default language should be German because we set quarkus.default-locale=de_DE
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("errors[0].message", containsString(ERROR_MESSAGE_GERMAN));
    }

    @Test
    public void testAcceptSpanish() {
        String query = getPayload("{echo(input:\"TOO LONG\")}");

        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .header(new Header("Accept-Language", "es"))
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("errors[0].message", containsString(ERROR_MESSAGE_SPANISH));
    }

    @Test
    public void testDefaultLocale() {
        String query = getPayload("{echo(input:\"TOO LONG\")}");

        // default language should be German because we set quarkus.default-locale=de_DE
        RestAssured.given()
                .body(query)
                .contentType(MEDIATYPE_JSON)
                .post("/graphql")
                .then()
                .assertThat()
                .statusCode(200)
                .and()
                .body("errors[0].message", containsString(ERROR_MESSAGE_GERMAN));
    }

}
