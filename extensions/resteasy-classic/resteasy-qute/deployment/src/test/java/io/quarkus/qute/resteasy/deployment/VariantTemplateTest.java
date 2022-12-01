package io.quarkus.qute.resteasy.deployment;

import static io.restassured.RestAssured.given;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.resteasy.deployment.ItemResource.Item;
import io.quarkus.test.QuarkusUnitTest;

public class VariantTemplateTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ItemResource.class, Item.class)
                    .addAsResource(new StringAsset("Item {item.name}: {item.price}"), "templates/ItemResource/item.txt")
                    .addAsResource(new StringAsset("<html><body>Item {item.name}: {item.price}</body></html>"),
                            "templates/ItemResource/item.html"));

    @Test
    public void testVariant() {
        given().when().accept("text/plain").get("/item/10").then().body(Matchers.is("Item foo: 10"));
        given().when().accept("text/html").get("/item/20").then().body(Matchers.is("<html><body>Item foo: 20</body></html>"));
    }

}
