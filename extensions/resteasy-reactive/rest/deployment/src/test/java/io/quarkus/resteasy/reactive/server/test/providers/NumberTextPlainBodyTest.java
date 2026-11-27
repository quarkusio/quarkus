package io.quarkus.resteasy.reactive.server.test.providers;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.quarkus.test.QuarkusExtensionTest;

public class NumberTextPlainBodyTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Resource.class));

    static Stream<Arguments> numberTypes() {
        return Stream.of(
                Arguments.of("/number/primitive/byte", "42", "42"),
                Arguments.of("/number/primitive/short", "42", "42"),
                Arguments.of("/number/primitive/int", "42", "42"),
                Arguments.of("/number/primitive/long", "42", "42"),
                Arguments.of("/number/primitive/float", "42.5", "42.5"),
                Arguments.of("/number/primitive/double", "42.5", "42.5"),
                Arguments.of("/number/wrapper/byte", "42", "42"),
                Arguments.of("/number/wrapper/short", "42", "42"),
                Arguments.of("/number/wrapper/integer", "42", "42"),
                Arguments.of("/number/wrapper/long", "42", "42"),
                Arguments.of("/number/wrapper/float", "42.5", "42.5"),
                Arguments.of("/number/wrapper/double", "42.5", "42.5"),
                Arguments.of("/number/big-integer", "42", "42"),
                Arguments.of("/number/big-decimal", "42.5", "42.5"));
    }

    @ParameterizedTest
    @MethodSource("numberTypes")
    public void testValidBody(String path, String body, String expected) {
        given().contentType("text/plain").body(body)
                .post(path).then().statusCode(200).body(is(expected));
    }

    @ParameterizedTest
    @MethodSource("numberTypes")
    public void testInvalidBody(String path, String ignoredBody, String ignoredExpected) {
        given().contentType("text/plain").body("abc")
                .post(path).then().statusCode(400);
    }

    @Path("/number")
    public static class Resource {

        @POST
        @Path("/primitive/byte")
        @Consumes(MediaType.TEXT_PLAIN)
        public String primitiveByte(byte value) {
            return Byte.toString(value);
        }

        @POST
        @Path("/primitive/short")
        @Consumes(MediaType.TEXT_PLAIN)
        public String primitiveShort(short value) {
            return Short.toString(value);
        }

        @POST
        @Path("/primitive/int")
        @Consumes(MediaType.TEXT_PLAIN)
        public String primitiveInt(int value) {
            return Integer.toString(value);
        }

        @POST
        @Path("/primitive/long")
        @Consumes(MediaType.TEXT_PLAIN)
        public String primitiveLong(long value) {
            return Long.toString(value);
        }

        @POST
        @Path("/primitive/float")
        @Consumes(MediaType.TEXT_PLAIN)
        public String primitiveFloat(float value) {
            return Float.toString(value);
        }

        @POST
        @Path("/primitive/double")
        @Consumes(MediaType.TEXT_PLAIN)
        public String primitiveDouble(double value) {
            return Double.toString(value);
        }

        @POST
        @Path("/wrapper/byte")
        @Consumes(MediaType.TEXT_PLAIN)
        public String wrapperByte(Byte value) {
            return value.toString();
        }

        @POST
        @Path("/wrapper/short")
        @Consumes(MediaType.TEXT_PLAIN)
        public String wrapperShort(Short value) {
            return value.toString();
        }

        @POST
        @Path("/wrapper/integer")
        @Consumes(MediaType.TEXT_PLAIN)
        public String wrapperInteger(Integer value) {
            return value.toString();
        }

        @POST
        @Path("/wrapper/long")
        @Consumes(MediaType.TEXT_PLAIN)
        public String wrapperLong(Long value) {
            return value.toString();
        }

        @POST
        @Path("/wrapper/float")
        @Consumes(MediaType.TEXT_PLAIN)
        public String wrapperFloat(Float value) {
            return value.toString();
        }

        @POST
        @Path("/wrapper/double")
        @Consumes(MediaType.TEXT_PLAIN)
        public String wrapperDouble(Double value) {
            return value.toString();
        }

        @POST
        @Path("/big-integer")
        @Consumes(MediaType.TEXT_PLAIN)
        public String bigInteger(BigInteger value) {
            return value.toString();
        }

        @POST
        @Path("/big-decimal")
        @Consumes(MediaType.TEXT_PLAIN)
        public String bigDecimal(BigDecimal value) {
            return value.toString();
        }
    }
}
