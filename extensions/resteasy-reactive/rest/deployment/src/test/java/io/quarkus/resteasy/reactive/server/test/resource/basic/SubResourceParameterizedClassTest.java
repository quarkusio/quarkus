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

class SubResourceParameterizedClassTest {
    @RegisterExtension
    static QuarkusUnitTest testExtension = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    JavaArchive war = ShrinkWrap.create(JavaArchive.class);
                    war.addClasses(PortProviderUtil.class);
                    war.addClasses(LanguageResourceImpl.class);
                    war.addClasses(LanguageResource.class);
                    war.addClasses(EnglishGreeterResource.class);
                    war.addClasses(GreeterResource.class);
                    return war;
                }
            });

    @Test
    void basicTest() {
        given().when().get("/languages/en").then().statusCode(200).body(is("hello"));
    }

    @Path("languages")
    public static class LanguageResourceImpl implements LanguageResource {

        private final Map<String, GreeterResource<?>> languages;

        @Inject
        public LanguageResourceImpl(final EnglishGreeterResource english) {
            languages = Map.of("en", english);
        }

        @Override
        public GreeterResource<?> locateGreeter(final String language) {
            final var resource = languages.get(language);
            if (resource != null) {
                return resource;
            }
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    public interface LanguageResource {

        @Path("{language}")
        GreeterResource<?> locateGreeter(@PathParam("language") final String language);
    }

    public interface GreeterResource<T> {
        @GET
        @Produces(MediaType.TEXT_PLAIN)
        T greeting();
    }

    @RequestScoped
    public static class EnglishGreeterResource implements GreeterResource<String> {
        @GET
        public String greeting() {
            return "hello";
        }
    }

}
