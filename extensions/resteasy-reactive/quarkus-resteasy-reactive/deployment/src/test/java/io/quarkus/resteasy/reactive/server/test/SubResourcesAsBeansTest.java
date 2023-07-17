package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.get;

import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.HttpHeaders;

import org.hamcrest.Matchers;
import org.jboss.resteasy.reactive.RestPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.resteasy.reactive.server.spi.SubResourcesAsBeansBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class SubResourcesAsBeansTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .addBuildChainCustomizer(new Consumer<>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new SubResourcesAsBeansBuildItem());
                        }
                    }).produces(SubResourcesAsBeansBuildItem.class).build();
                }
            })
            .withApplicationRoot((jar) -> jar.addClasses(RestResource.class, MiddleRestResource.class, RestSubResource.class));

    @Test
    public void testSubResource() {
        get("/sub-resource/Bob/Builder")
                .then()
                .body(Matchers.equalTo("Bob Builder"))
                .statusCode(200);
    }

    @Path("/")
    public static class RestResource {

        private final MiddleRestResource restSubResource;

        public RestResource(MiddleRestResource restSubResource) {
            this.restSubResource = restSubResource;
        }

        @Path("sub-resource/{first}")
        public MiddleRestResource hello(String first) {
            return restSubResource;
        }
    }

    public static class MiddleRestResource {

        @Inject
        RestSubResource restSubResource;

        @Path("{last}")
        public RestSubResource hello() {
            return restSubResource;
        }
    }

    public static class RestSubResource {

        @GET
        public String hello(HttpHeaders headers, @RestPath String first, @RestPath String last) {
            return first + " " + last;
        }
    }
}
