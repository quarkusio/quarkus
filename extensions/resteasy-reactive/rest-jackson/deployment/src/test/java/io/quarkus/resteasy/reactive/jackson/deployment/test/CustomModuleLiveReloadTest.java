package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import java.io.IOException;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import io.quarkus.jackson.ObjectMapperCustomizer;
import io.quarkus.test.QuarkusDevModeTest;
import io.vertx.core.json.JsonArray;

public class CustomModuleLiveReloadTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class, StringAndInt.class, StringAndIntSerializer.class,
                            StringAndIntDeserializer.class, Customizer.class)
                    .addAsResource(new StringAsset("index content"), "META-INF/resources/index.html"));

    @Test
    void test() {
        assertResponse();

        // force reload
        TEST.addResourceFile("META-INF/resources/index.html", "html content");

        assertResponse();
    }

    private static void assertResponse() {
        given().accept("application/json").get("test/array")
                .then()
                .statusCode(200)
                .body(containsString("first:1"), containsString("second:2"));
    }

    @Path("test")
    public static class Resource {

        @Path("array")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public JsonArray array() {
            var array = new JsonArray();
            array.add(new StringAndInt("first", 1));
            array.add(new StringAndInt("second", 2));
            return array;
        }
    }

    public static class StringAndInt {
        private final String stringValue;
        private final int intValue;

        public StringAndInt(String s, int i) {
            this.stringValue = s;
            this.intValue = i;
        }

        public static StringAndInt parse(String value) {
            if (value == null) {
                return null;
            }
            int dot = value.indexOf(':');
            if (-1 == dot) {
                throw new IllegalArgumentException(value);
            }
            try {
                return new StringAndInt(value.substring(0, dot), Integer.parseInt(value.substring(dot + 1)));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(value, e);
            }
        }

        public String format() {
            return this.stringValue + ":" + intValue;
        }
    }

    public static class StringAndIntSerializer extends StdSerializer<StringAndInt> {

        public StringAndIntSerializer() {
            super(StringAndInt.class);
        }

        @Override
        public void serialize(StringAndInt value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            if (value == null)
                gen.writeNull();
            else {
                gen.writeString(value.format());
            }
        }
    }

    public static class StringAndIntDeserializer extends StdDeserializer<StringAndInt> {

        public StringAndIntDeserializer() {
            super(StringAndInt.class);
        }

        @Override
        public StringAndInt deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            if (p.currentToken() == JsonToken.VALUE_STRING) {
                return StringAndInt.parse(p.getText());
            } else if (p.currentToken() == JsonToken.VALUE_NULL) {
                return null;
            }
            return null;
        }
    }

    @Singleton
    public static class Customizer implements ObjectMapperCustomizer {
        @Override
        public void customize(ObjectMapper objectMapper) {
            var m = new SimpleModule("test");
            m.addSerializer(StringAndInt.class, new StringAndIntSerializer());
            m.addDeserializer(StringAndInt.class, new StringAndIntDeserializer());
            objectMapper.registerModule(m);
        }
    }
}
