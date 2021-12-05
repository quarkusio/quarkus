package io.quarkus.qute.deployment.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusUnitTest;

public class UserTagTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{it}"), "templates/tags/hello.txt")
                    .addAsResource(new StringAsset("{#hello name /}"), "templates/foo.txt"));

    @Inject
    Template foo;

    @Test
    public void testInjection() {
        assertEquals("bar", foo.data("name", "bar").render());
    }

}
