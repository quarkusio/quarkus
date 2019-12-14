package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateData;
import io.quarkus.test.QuarkusUnitTest;

public class EscapingTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(Item.class)
                    .addAsResource(new StringAsset("{text} {other} {text.raw} {text.safe} {item.foo}"),
                            "templates/foo.html")
                    .addAsResource(new StringAsset("{item} {item.raw}"),
                            "templates/item.html")
                    .addAsResource(new StringAsset("{text} {other} {text.raw} {text.safe} {item.foo}"),
                            "templates/bar.txt"));

    @Inject
    Template foo;

    @Inject
    Template bar;

    @Inject
    Template item;

    @Test
    public void testEscaper() {
        assertEquals("&lt;div&gt; &amp;&quot;&#39; <div> <div> <span>",
                foo.data("text", "<div>").data("other", "&\"'").data("item", new Item()).render());
        // No escaping for txt templates
        assertEquals("<div> &\"' <div> <div> <span>",
                bar.data("text", "<div>").data("other", "&\"'").data("item", new Item()).render());
        // Item.toString() is escaped too
        assertEquals("&lt;h1&gt;Item&lt;/h1&gt; <h1>Item</h1>",
                item.data("item", new Item()).render());
    }

    @TemplateData
    public static class Item {

        public RawString getFoo() {
            return new RawString("<span>");
        }

        @Override
        public String toString() {
            return "<h1>Item</h1>";
        }

    }

}
