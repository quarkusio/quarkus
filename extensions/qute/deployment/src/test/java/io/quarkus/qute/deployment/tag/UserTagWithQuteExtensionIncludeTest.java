package io.quarkus.qute.deployment.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

public class UserTagWithQuteExtensionIncludeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{name}"),
                            "templates/tags/hello.qute.txt")
                    .addAsResource(new StringAsset("{#include base}{#item}{#hello name=name /}{/item}{/include}"),
                            "templates/foo.txt")
                    .addAsResource(new StringAsset("{#insert item}NOK{/}"),
                            "templates/base.html"));

    @Inject
    Template foo;

    @Test
    public void testInjection() {
        assertEquals("OK", foo.data("name", "OK").render());
    }

}
