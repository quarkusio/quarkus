package io.quarkus.qute.deployment.typesafe.fragment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateException;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class MissingParamCheckedTemplateFragmentTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClasses(Templates.class, Item.class).addAsResource(
                    new StringAsset("{#each items}{#fragment id='item'}{it.name}{/fragment}{/each}"),
                    "templates/MissingParamCheckedTemplateFragmentTest/items.html"))
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
                assertEquals(
                        "MissingParamCheckedTemplateFragmentTest$Templates#items$item() must declare a parameter of name [it] and type [io.quarkus.qute.deployment.typesafe.fragment.Item]",
                        te.getMessage());
            });;

    @Test
    public void test() {
        fail();
    }

    @CheckedTemplate
    public static class Templates {

        static native TemplateInstance items(List<Item> items);

        static native TemplateInstance items$item();
    }

}
