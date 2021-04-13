package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateRequireTypeSafeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Templates.class)
                    .addAsResource(new StringAsset("Hello {name}! {any}"),
                            "templates/CheckedTemplateRequireTypeSafeTest/hola.txt"))
            .setExpectedException(TemplateException.class);

    @Test
    public void testValidation() {
        fail();
    }

    @CheckedTemplate // requireTypeSafeExpressions=true by default 
    static class Templates {

        static native TemplateInstance hola(String name);

    }

}
