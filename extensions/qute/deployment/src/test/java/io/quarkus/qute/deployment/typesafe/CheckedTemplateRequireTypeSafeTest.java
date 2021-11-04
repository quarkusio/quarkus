package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import javax.inject.Named;
import javax.inject.Singleton;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class CheckedTemplateRequireTypeSafeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Templates.class, Fool.class)
                    .addAsResource(new StringAsset(
                            "Hello {name}!"
                                    + "{any} "
                                    + "{inject:fool.getJoke(null)} "
                                    + "{inject:fool.getJoke(identifier)} "
                                    + "{#each name.chars.iterator}"
                                    // {it_index} is not considered an error because the binding is registered by the loop section !}
                                    + "{it_index}."
                                    // however, {index} is an error
                                    + "{index}"
                                    + "{it}"
                                    + "{/each}"),
                            "templates/CheckedTemplateRequireTypeSafeTest/hola.txt"))
            .assertException(t -> {
                Throwable e = t;
                TemplateException te = null;
                while (e != null) {
                    if (e instanceof TemplateException) {
                        te = (TemplateException) e;
                        break;
                    }
                    e = e.getCause();
                }
                assertNotNull(te);
                assertTrue(te.getMessage().contains("Found template problems (3)"), te.getMessage());
                assertTrue(te.getMessage().contains("any"), te.getMessage());
                assertTrue(te.getMessage().contains("identifier"), te.getMessage());
                assertTrue(te.getMessage().contains("index"), te.getMessage());
            });

    @Test
    public void testValidation() {
        fail();
    }

    @CheckedTemplate // requireTypeSafeExpressions=true by default 
    static class Templates {

        static native TemplateInstance hola(String name);

    }

    @Singleton
    @Named
    public static class Fool {

        public String getJoke(Integer id) {
            return "ok";
        }

    }

}
