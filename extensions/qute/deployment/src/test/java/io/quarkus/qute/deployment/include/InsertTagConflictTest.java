package io.quarkus.qute.deployment.include;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.TemplateException;
import io.quarkus.test.QuarkusUnitTest;

public class InsertTagConflictTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addAsResource(new StringAsset("{#insert row}{/}"), "templates/base.html")
                    .addAsResource(new StringAsset("Nuke!"), "templates/tags/row.html"))
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
                assertTrue(te.getMessage().contains(
                        "Parser error in template [base.html] line 1: {#insert} defined in the {#include} conflicts with an existing section/tag: row"),
                        te.getMessage());
            });;

    @Test
    public void test() {
        fail();
    }
}
