package io.quarkus.qute.deployment.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * {@code quarkus.qute.validate-user-tag-arguments=false} restores the behaviour of applications that pass arguments
 * which do not match the parameter declarations of the tag template: the build succeeds and the value is rendered as
 * it was before the validation existed.
 */
public class UserTagArgumentTypeValidationDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{@java.lang.Integer count}{count}"), "templates/tags/hello.txt")
                    .addAsResource(new StringAsset("{@java.lang.String name}{#hello count=name /}"),
                            "templates/foo.txt"))
            .overrideConfigKey("quarkus.qute.validate-user-tag-arguments", "false");

    @Inject
    Engine engine;

    @Test
    public void theIncompatibleArgumentIsStillRendered() {
        assertEquals("ten", engine.getTemplate("foo").data("name", "ten").render());
    }

}
