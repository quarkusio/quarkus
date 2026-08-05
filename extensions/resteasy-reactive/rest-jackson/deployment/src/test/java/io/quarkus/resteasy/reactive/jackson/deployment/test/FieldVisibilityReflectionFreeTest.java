package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.function.Supplier;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import io.quarkus.jackson.JsonMapperBuilderCustomizer;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import tools.jackson.databind.json.JsonMapper;

public class FieldVisibilityReflectionFreeTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(FieldOnlyVisibilityCustomizer.class,
                                    Item.class,
                                    ItemResource.class)
                            .addAsResource(
                                    new StringAsset(
                                            "quarkus.rest.jackson.optimization.enable-reflection-free-serializers=true\n"),
                                    "application.properties");
                }
            });

    @Test
    public void testSerializationUsesFieldNotGetter() {
        RestAssured.get("/field-visibility")
                .then()
                .statusCode(200)
                .body("secret", is("hidden"))
                .body(not(containsString("\"exposed\"")));
    }

    @Test
    public void testRoundTrip() {
        RestAssured
                .with()
                .body("{\"secret\": \"value\"}")
                .contentType("application/json")
                .post("/field-visibility")
                .then()
                .statusCode(200)
                .body("secret", is("value"))
                .body(not(containsString("\"exposed\"")));
    }

    @Singleton
    public static class FieldOnlyVisibilityCustomizer implements JsonMapperBuilderCustomizer {
        @Override
        public void customize(JsonMapper.Builder mapper) {
            mapper.changeDefaultVisibility(vc -> vc
                    .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                    .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                    .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        }
    }

    public static class Item {
        private String secret;

        public Item() {
        }

        public Item(String secret) {
            this.secret = secret;
        }

        public String getExposed() {
            return secret;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }
    }

    @Path("/field-visibility")
    public static class ItemResource {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public Item get() {
            return new Item("hidden");
        }

        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Item echo(Item item) {
            return item;
        }
    }
}
