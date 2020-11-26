package io.quarkus.qute.rest.deployment;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.rest.deployment.ItemResource.Item;
import io.quarkus.test.QuarkusUnitTest;

public class VariantTemplateTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(ItemResource.class, Item.class)
                    .addAsResource(new StringAsset("Item {name}: {price}"), "templates/item.txt")
                    .addAsResource(new StringAsset("<html><body>Item {name}: {price}</body></html>"),
                            "templates/item.html"));

    @Test
    public void testVariant() {
        given().when().accept("text/plain").get("/item/10").then().body(Matchers.is("Item foo: 10"));
        given().when().get("/item/20").then().body(Matchers.is("<html><body>Item foo: 20</body></html>"));
    }

}
