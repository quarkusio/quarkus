package io.quarkus.resteasy.reactive.qute.deployment;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.BlockingOperationControl;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateInstanceNonBlockingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class)
                    .addAsResource(new StringAsset("Blocking allowed: {blockingAllowed}"), "templates/item.txt"));

    @Test
    public void test() {
        when().get("/test").then().statusCode(200).body(Matchers.is("Blocking allowed: false"));
    }

    @Path("test")
    public static class TestResource {

        @Inject
        Template item;

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public TemplateInstance get() {
            return item.data("blockingAllowed", BlockingOperationControl.isBlockingAllowed());
        }
    }
}
