package io.quarkus.resteasy.reactive.server.test.resource.basic;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.simple.PortProviderUtil;
import io.quarkus.test.QuarkusUnitTest;

class SubResourceAmbiguousInjectTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClass(PortProviderUtil.class);
                    war.addClass(EnglishGreeterResource.class);
                    war.addClass(SpanishGreeterResource.class);
                    war.addClass(GreeterResource.class);
                    war.addClass(LanguageResource.class);
                    war.addClass(LanguageResourceV2.class);
                    war.addClass(GreeterResourceV2.class);
                    war.addClass(EnglishGreeterResource.class);
                    war.addClass(SpanishGreeterResource.class);
                    return war;
                }
            });

    @Test
    void basicTest() {
        given().when().get("/languages/en").then().statusCode(200).body(is("hello"));
        given().when().get("/languages/v2/es").then().statusCode(200).body(is("hola"));
    }

    @RequestScoped
    public static class EnglishGreeterResource implements GreeterResource {
        @Override
        public String greeting() {
            return "hello";
        }
    }

    @RequestScoped
    public static class SpanishGreeterResource implements GreeterResource {
        @Override
        public String greeting() {
            return "hola";
        }
    }

    public interface GreeterResource {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        String greeting();
    }

    @Path("languages")
    public static class LanguageResource {

        private final Map<String, GreeterResource> languages;

        @Inject
        public LanguageResource(
                final EnglishGreeterResource english, final SpanishGreeterResource spanish) {
            languages = Map.of("en", english, "es", spanish);
        }

        @Path("{language}")
        public GreeterResource locateGreeter(@PathParam("language") final String language) {
            return languages.get(language);
        }
    }

    @Path("languages/v2")
    public static class LanguageResourceV2 {

        private final Map<String, GreeterResourceV2> languages;

        @Inject
        public LanguageResourceV2(
                final EnglishGreeterResourceV2 english, final SpanishGreeterResourceV2 spanish) {
            languages = Map.of("en", english, "es", spanish);
        }

        @Path("{language}")
        public GreeterResourceV2 locateGreeter(@PathParam("language") final String language) {
            return languages.get(language);
        }
    }

    public abstract static class GreeterResourceV2 {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public abstract String greeting();
    }

    @RequestScoped
    public static class EnglishGreeterResourceV2 extends GreeterResourceV2 {
        @Override
        public String greeting() {
            return "hello";
        }
    }

    @RequestScoped
    public static class SpanishGreeterResourceV2 extends GreeterResourceV2 {
        @Override
        public String greeting() {
            return "hola";
        }
    }
}
