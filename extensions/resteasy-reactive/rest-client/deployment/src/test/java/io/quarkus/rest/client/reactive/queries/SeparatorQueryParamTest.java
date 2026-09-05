package io.quarkus.rest.client.reactive.queries;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.jboss.resteasy.reactive.RestQuery;
import org.jboss.resteasy.reactive.Separator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;

public class SeparatorQueryParamTest {
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(SeparatorQueryParamTest.Resource.class));

    @TestHTTPResource
    URI baseUri;

    @Test
    void shouldJoinOnlyForMultipleSizedRestQuery() {
        Client client = createClient();
        assertThat(client.restQuery(List.of())).isEqualTo("none");
        assertThat(client.restQuery(List.of("QUARKUS"))).isEqualTo("frameworks=QUARKUS");
        //%2C - comma
        assertThat(client.restQuery(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks=QUARKUS%2CHELIDON");
    }

    @Test
    void shouldWorkForQueryParamSame() {
        Client client = createClient();
        //%2C - comma
        assertThat(client.queryParam(List.of("QUARKUS", "HELIDON"))).isEqualTo("frameworks=QUARKUS%2CHELIDON");
    }

    @Test
    void shouldConvertParamBeforeDoingJoin() {
        Client client = createClient();
        assertThat(client.queryParamConvertible(
                List.of(JavaFramework.QUARKUS, JavaFramework.SPRING, JavaFramework.HELIDON, JavaFramework.JAVALIN)))
                .isEqualTo("frameworks=QUARKUS%2CSPRING%2CHELIDON%2CJAVALIN");
    }

    /*
     * MultiQueryParamMode.MULTI_PAIRS is default, so we should get &javaVersions=21&javaVersions=25 for second parameter
     */
    @Test
    void shouldApplyOnlyForAnnotated() {
        Client client = createClient();
        assertThat(client.mixed(
                List.of(JavaFramework.QUARKUS, JavaFramework.SPRING, JavaFramework.HELIDON, JavaFramework.JAVALIN),
                List.of("21", "25")))
                .isEqualTo("frameworks=QUARKUS%2CSPRING%2CHELIDON%2CJAVALIN&javaVersions=21&javaVersions=25");
    }

    enum JavaFramework {
        QUARKUS,
        SPRING,
        JAVALIN,
        HELIDON
    }

    public static class JavaFrameworkParamConverter implements ParamConverter<SeparatorQueryParamTest.JavaFramework> {

        @Override
        public SeparatorQueryParamTest.JavaFramework fromString(String value) {
            return Enum.valueOf(SeparatorQueryParamTest.JavaFramework.class, value);
        }

        @Override
        public String toString(SeparatorQueryParamTest.JavaFramework jf) {
            return jf != null ? jf.toString() : null;
        }

    }

    public static class JavaFrameworkConverterProvider implements ParamConverterProvider {

        @Override
        public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
            if (SeparatorQueryParamTest.JavaFramework.class.isAssignableFrom(rawType)) {
                return (ParamConverter<T>) new SeparatorQueryParamTest.JavaFrameworkParamConverter();
            }

            return null;
        }
    }

    @RegisterProvider(JavaFrameworkConverterProvider.class)
    public interface Client {
        @GET
        @Path("/separator/query")
        String restQuery(@RestQuery @Separator(",") List<String> frameworks);

        @GET
        @Path("/separator/query")
        String queryParam(@QueryParam("frameworks") @Separator(",") List<String> frameworks);

        @GET
        @Path("/separator/query")
        String queryParamConvertible(@RestQuery("frameworks") @Separator(",") List<JavaFramework> frameworks);

        @GET
        @Path("/separator/query")
        String mixed(@RestQuery @Separator(",") List<JavaFramework> frameworks, @RestQuery List<String> javaVersions);
    }

    Client createClient() {
        return RestClientBuilder.newBuilder().baseUri(baseUri).build(Client.class);
    }

    @Path("/separator")
    @ApplicationScoped
    public static class Resource {

        @GET
        @Path("/query")
        public String query(@Context UriInfo info) {
            if (info.getQueryParameters().isEmpty())
                return "none";

            return info.getRequestUri().getRawQuery();
        }
    }

}
