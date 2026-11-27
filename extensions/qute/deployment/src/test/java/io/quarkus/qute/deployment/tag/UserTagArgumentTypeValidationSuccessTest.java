package io.quarkus.qute.deployment.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Arguments that match the parameter declarations of a user tag, including widening numeric conversions, subtypes,
 * arguments without type information and undeclared arguments, are accepted.
 */
public class UserTagArgumentTypeValidationSuccessTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{@java.lang.Long size}{@boolean flag}{@java.lang.CharSequence text}"
                            + "{@java.lang.Integer count=1}{@java.lang.Number it}"
                            + "{it}:{size}:{flag}:{text}:{count};"), "templates/tags/hello.txt")
                    .addAsResource(new StringAsset("{@java.lang.String str}{@int num}{@java.lang.Integer boxed}"
                            + "{#hello num size=20 flag=true text=str /}"
                            + "{#hello boxed size=num flag=false text='lit' count=3 extra=true /}"
                            + "{#hello num size=unknown flag=true text='x' /}"), "templates/foo.txt"));

    @Inject
    Template foo;

    @Test
    public void testRendering() {
        assertEquals("5:20:true:hi:1;7:5:false:lit:3;5:9:true:x:1;",
                foo.data("str", "hi", "num", 5, "boxed", 7, "unknown", 9L).render());
    }

}
