package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateEnum;
import io.quarkus.test.QuarkusUnitTest;

public class ParamDeclarationDefaultValueTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEnum.class)
                    .addAsResource(new StringAsset(
                            "{@io.quarkus.qute.deployment.typesafe.ParamDeclarationDefaultValueTest$MyEnum myEnum=MyEnum:BAR}{myEnum}"),
                            "templates/myEnum.html"));

    @Inject
    Template myEnum;

    @Test
    public void testDefaultValue() {
        assertEquals("BAR", myEnum.render());
    }

    @TemplateEnum
    enum MyEnum {
        FOO,
        BAR
    }
}
