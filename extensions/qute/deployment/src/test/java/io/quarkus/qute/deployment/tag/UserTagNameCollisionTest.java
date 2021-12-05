package io.quarkus.qute.deployment.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusUnitTest;

public class UserTagNameCollisionTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("Hello from tag template!"), "templates/tags/hello.txt")
                    .addAsResource(new StringAsset("Hello from regular template!"), "templates/hello.txt"));

    @Inject
    Engine engine;

    @Test
    public void testTagDoesNotShadowRegularTemplate() {
        assertEquals("Hello from regular template!", engine.getTemplate("hello").render());
        assertEquals("Hello from tag template!", engine.getTemplate("tags/hello").render());
    }

}
