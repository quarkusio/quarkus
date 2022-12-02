package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.qute.deployment.Foo;
import io.quarkus.test.QuarkusUnitTest;

public class ParamDeclarationWrongClassTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(Foo.class)
                    .addAsResource(new StringAsset("{@org.acme.Foo foo}"
                            + "{foo.name}"), "templates/foo.html"))
            .setExpectedException(TemplateException.class);

    @Test
    public void testValidation() {
        fail();
    }

}
