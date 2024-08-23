package io.quarkus.resteasy.reactive.jackson.deployment.test;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.resteasy.reactive.jackson.SecureField;
import io.quarkus.resteasy.reactive.jackson.runtime.ResteasyReactiveServerJacksonRecorder;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.security.test.utils.TestIdentityController;
import io.quarkus.security.test.utils.TestIdentityProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

public class SecureFieldDetectionTest {

    private static final String SECURITY_SERIALIZATION = "security_serialization";

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MultiResource.class, UniResource.class, ObjectResource.class, ResponseResource.class,
                            PlainResource.class, TestIdentityProvider.class, TestIdentityController.class,
                            CollectionResource.class, NoSecureField.class, WithSecureField.class, WithNestedSecureField.class,
                            ResponseType.class, DetectSecuritySerializationHandler.class, JsonIgnoreDto.class))
            .addBuildChainCustomizer(buildChainBuilder -> buildChainBuilder.addBuildStep(context -> context.produce(
                    new MethodScannerBuildItem(new MethodScanner() {
                        @Override
                        public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                                Map<String, Object> methodContext) {
                            return List.of(new DetectSecuritySerializationHandler());
                        }
                    }))).produces(MethodScannerBuildItem.class).build());

    @BeforeEach
    public void setupSecurity() {
        TestIdentityController.resetRoles().add("Georgios", "Andrianakis", "admin");
    }

    private static Stream<Arguments> responseTypes() {
        return EnumSet.allOf(ResponseType.class).stream().map(Enum::toString).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("responseTypes")
    public void testSecureFieldDetection(String responseTypeStr) {
        var responseType = ResponseType.valueOf(responseTypeStr);

        // with auth
        RestAssured
                .given()
                .auth().preemptive().basic("Georgios", "Andrianakis")
                .pathParam("sub-path", responseType.getResourceSubPath())
                .get("/{sub-path}/secure-field")
                .then()
                .statusCode(200)
                .body(containsString("hush hush"));
        RestAssured
                .given()
                .auth().preemptive().basic("Georgios", "Andrianakis")
                .pathParam("sub-path", responseType.getResourceSubPath())
                .get("/{sub-path}/no-secure-field")
                .then()
                .statusCode(200)
                .body(containsString("public"));

        // no auth
        RestAssured
                .given()
                .pathParam("sub-path", responseType.getResourceSubPath())
                .get("/{sub-path}/secure-field")
                .then()
                .statusCode(200)
                .header(SECURITY_SERIALIZATION, is("true"))
                .body(not(containsString("hush hush")));

        // if endpoint returns for example Object or Response we can't really tell during the build time
        // therefore we add custom security serialization and let decision be made dynamically based on present annotation
        boolean isSecureSerializationApplied = !responseType.isSecureFieldDetectable();
        RestAssured
                .given()
                .pathParam("sub-path", responseType.getResourceSubPath())
                .get("/{sub-path}/no-secure-field")
                .then()
                .statusCode(200)
                .header(SECURITY_SERIALIZATION, is(Boolean.toString(isSecureSerializationApplied)))
                .body(containsString("public"));

        RestAssured
                .given()
                .pathParam("sub-path", responseType.getResourceSubPath())
                .get("/{sub-path}/json-ignore")
                .then()
                .statusCode(200)
                .header(SECURITY_SERIALIZATION, is(Boolean.toString(isSecureSerializationApplied)))
                .body(containsString("other"))
                .body(not(containsString("ignored")));
    }

    @Path("plain")
    public static class PlainResource {

        @Path("secure-field")
        @GET
        public WithNestedSecureField secureField() {
            return createEntityWithSecureField();
        }

        @Path("no-secure-field")
        @GET
        public NoSecureField noSecureField() {
            return createEntityWithoutSecureField();
        }

        @Path("json-ignore")
        @GET
        public JsonIgnoreDto jsonIgnore() {
            return createEntityWithSecureFieldInIgnored();
        }

    }

    @Path("multi")
    public static class MultiResource {

        @Path("secure-field")
        @GET
        public Multi<WithNestedSecureField> secureField() {
            return Multi.createFrom().item(createEntityWithSecureField());
        }

        @Path("no-secure-field")
        @GET
        public Multi<NoSecureField> noSecureField() {
            return Multi.createFrom().item(createEntityWithoutSecureField());
        }

        @Path("json-ignore")
        @GET
        public Multi<JsonIgnoreDto> jsonIgnore() {
            return Multi.createFrom().item(createEntityWithSecureFieldInIgnored());
        }

    }

    @Path("collection")
    public static class CollectionResource {

        @Path("secure-field")
        @GET
        public Collection<WithNestedSecureField> secureField() {
            return List.of(createEntityWithSecureField());
        }

        @Path("no-secure-field")
        @GET
        public Collection<NoSecureField> noSecureField() {
            return Set.of(createEntityWithoutSecureField());
        }

        @Path("json-ignore")
        @GET
        public Collection<JsonIgnoreDto> jsonIgnore() {
            return Set.of(createEntityWithSecureFieldInIgnored());
        }

    }

    @Path("uni")
    public static class UniResource {

        @Path("secure-field")
        @GET
        public Uni<WithNestedSecureField> secureField() {
            return Uni.createFrom().item(createEntityWithSecureField());
        }

        @Path("no-secure-field")
        @GET
        public Uni<NoSecureField> noSecureField() {
            return Uni.createFrom().item(createEntityWithoutSecureField());
        }

        @Path("json-ignore")
        @GET
        public Uni<JsonIgnoreDto> jsonIgnore() {
            return Uni.createFrom().item(createEntityWithSecureFieldInIgnored());
        }

    }

    @Produces(APPLICATION_JSON)
    @Path("object")
    public static class ObjectResource {

        @Path("secure-field")
        @GET
        public Object secureField() {
            return createEntityWithSecureField();
        }

        @Path("no-secure-field")
        @GET
        public Object noSecureField() {
            return createEntityWithoutSecureField();
        }

        @Path("json-ignore")
        @GET
        public Object jsonIgnore() {
            return createEntityWithSecureFieldInIgnored();
        }

    }

    @Produces(APPLICATION_JSON)
    @Path("response")
    public static class ResponseResource {

        @Path("secure-field")
        @GET
        public Response secureField() {
            return Response.ok(createEntityWithSecureField()).build();
        }

        @Path("no-secure-field")
        @GET
        public Response noSecureField() {
            return Response.ok(createEntityWithoutSecureField()).build();
        }

        @Path("json-ignore")
        @GET
        public Response jsonIgnore() {
            return Response.ok(createEntityWithSecureFieldInIgnored()).build();
        }

    }

    @Path("rest-response")
    public static class RestResponseResource {

        @Path("secure-field")
        @GET
        public RestResponse<WithNestedSecureField> secureField() {
            return RestResponse.ok(createEntityWithSecureField());
        }

        @Path("no-secure-field")
        @GET
        public RestResponse<NoSecureField> noSecureField() {
            return RestResponse.ok(createEntityWithoutSecureField());
        }

        @Path("json-ignore")
        @GET
        public RestResponse<JsonIgnoreDto> jsonIgnore() {
            return RestResponse.ok(createEntityWithSecureFieldInIgnored());
        }

    }

    private static NoSecureField createEntityWithoutSecureField() {
        var resp = new NoSecureField();
        resp.setNotSecured("public");
        return resp;
    }

    private static JsonIgnoreDto createEntityWithSecureFieldInIgnored() {
        var resp = new JsonIgnoreDto();
        resp.setOtherField("other");
        var nested = new WithSecureField();
        nested.setSecured("ignored");
        resp.setWithSecureField(nested);
        return resp;
    }

    private static WithNestedSecureField createEntityWithSecureField() {
        var resp = new WithNestedSecureField();
        var nested = new WithSecureField();
        nested.setSecured("hush hush");
        resp.setWithSecureField(nested);
        return resp;
    }

    public static class JsonIgnoreDto {

        @JsonIgnore
        private WithSecureField withSecureField;

        private String otherField;

        public WithSecureField getWithSecureField() {
            return withSecureField;
        }

        public void setWithSecureField(WithSecureField withSecureField) {
            this.withSecureField = withSecureField;
        }

        public String getOtherField() {
            return otherField;
        }

        public void setOtherField(String otherField) {
            this.otherField = otherField;
        }
    }

    public static class NoSecureField {

        private String notSecured;

        public String getNotSecured() {
            return notSecured;
        }

        public void setNotSecured(String notSecured) {
            this.notSecured = notSecured;
        }
    }

    public static class WithNestedSecureField {

        private WithSecureField withSecureField;

        public WithSecureField getWithSecureField() {
            return withSecureField;
        }

        public void setWithSecureField(WithSecureField withSecureField) {
            this.withSecureField = withSecureField;
        }
    }

    public static class WithSecureField {

        @SecureField(rolesAllowed = "admin")
        private String secured;

        public String getSecured() {
            return secured;
        }

        public void setSecured(String secured) {
            this.secured = secured;
        }
    }

    public static class DetectSecuritySerializationHandler implements ServerRestHandler, HandlerChainCustomizer {

        @Override
        public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
            var methodId = requestContext.getResteasyReactiveResourceInfo().getMethodId();
            var customSerialization = ResteasyReactiveServerJacksonRecorder.customSerializationForMethod(methodId);
            var customSerializationDetected = Boolean.toString(customSerialization != null);
            requestContext.unwrap(RoutingContext.class).response().putHeader(SECURITY_SERIALIZATION,
                    customSerializationDetected);
        }

        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
            return List.of(new DetectSecuritySerializationHandler());
        }
    }
}
