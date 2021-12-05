package io.quarkus.smallrye.openapi.test.jaxrs;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class ConfigMappingTest {
    private static final String OPEN_API_PATH = "/q/openapi";

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DefaultContentTypeResource.class, Greeting.class)

                    .addAsResource(new StringAsset("quarkus.smallrye-openapi.open-api-version=3.0.3\n"
                            + "quarkus.smallrye-openapi.servers=http:\\//www.server1.com,http:\\//www.server2.com\n"
                            + "quarkus.smallrye-openapi.info-title=My API\n"
                            + "quarkus.smallrye-openapi.info-version=1.2.3\n"
                            + "quarkus.smallrye-openapi.info-description=My Description\n"
                            + "quarkus.smallrye-openapi.info-terms-of-service=Some terms\n"
                            + "quarkus.smallrye-openapi.info-contact-email=my.email@provider.com\n"
                            + "quarkus.smallrye-openapi.info-contact-name=My Name\n"
                            + "quarkus.smallrye-openapi.info-contact-url=http:\\//www.foo.bar\n"
                            + "quarkus.smallrye-openapi.info-license-name=Some License\n"
                            + "quarkus.smallrye-openapi.info-license-url=http:\\//www.somelicense.com\n"
                            + "quarkus.smallrye-openapi.operation-id-strategy=package-class-method"),
                            "application.properties"));

    @Test
    public void testOpenApiPathAccessResource() {
        RestAssured.given().queryParam("format", "JSON")
                .when().get(OPEN_API_PATH)
                .then()
                .log().body().and()
                .body("openapi",
                        Matchers.equalTo("3.0.3"))
                .body("servers[0].url",
                        Matchers.startsWith("http://www.server"))
                .body("servers[1].url",
                        Matchers.startsWith("http://www.server"))
                .body("info.title",
                        Matchers.equalTo("My API"))
                .body("info.description",
                        Matchers.equalTo("My Description"))
                .body("info.termsOfService",
                        Matchers.equalTo("Some terms"))
                .body("info.contact.name",
                        Matchers.equalTo("My Name"))
                .body("info.contact.url",
                        Matchers.equalTo("http://www.foo.bar"))
                .body("info.contact.email",
                        Matchers.equalTo("my.email@provider.com"))
                .body("info.license.name",
                        Matchers.equalTo("Some License"))
                .body("info.license.url",
                        Matchers.equalTo("http://www.somelicense.com"))
                .body("info.version",
                        Matchers.equalTo("1.2.3"))
                .body("paths.'/greeting/goodbye'.get.operationId",
                        Matchers.equalTo("io.quarkus.smallrye.openapi.test.jaxrs.DefaultContentTypeResource_byebye"));

    }
}
