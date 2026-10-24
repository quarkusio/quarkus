package io.quarkus.qute.deployment.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

public class UserTagArgumentTypeValidationVariantsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Item.class)
                    .addAsResource(new StringAsset("{@java.lang.Integer count}<b>{count}</b>"), "templates/tags/item.html")
                    .addAsResource(new StringAsset("{@java.lang.String count}{count}"), "templates/tags/item.txt")
                    .addAsResource(new StringAsset("{@java.lang.Integer count}{count}"), "templates/tags/my.item.txt")
                    .addAsResource(new StringAsset("{@java.lang.String count}{count}"), "templates/tags/my.txt")
                    .addAsResource(new StringAsset("{@java.lang.Integer count}{count}"), "templates/tags/excluded.txt")
                    .addAsResource(new StringAsset("{@java.lang.String str}{#item count=str /}|{#my count=str /}"),
                            "templates/foo.txt")
                    .addAsResource(new StringAsset(
                            "{@io.quarkus.qute.deployment.tag.UserTagArgumentTypeValidationVariantsTest$Item item}"
                                    + "{#excluded count=item.missing /}"),
                            "templates/bar.txt")
                    .addAsResource(new StringAsset(
                            "quarkus.qute.type-check-excludes=io.quarkus.qute.deployment.tag.UserTagArgumentTypeValidationVariantsTest$Item.missing"),
                            "application.properties"));

    @Inject
    Template foo;

    @Test
    public void testRendering() {
        assertEquals("<b>hi</b>|hi", foo.data("str", "hi").render());
    }

    public static class Item {

        public String name;

    }

}
