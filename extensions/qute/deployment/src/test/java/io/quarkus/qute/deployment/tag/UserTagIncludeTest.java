package io.quarkus.qute.deployment.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class UserTagIncludeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{name}"),
                            "templates/tags/hello.txt")
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
