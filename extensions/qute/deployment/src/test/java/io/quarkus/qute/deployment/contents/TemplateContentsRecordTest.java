package io.quarkus.qute.deployment.contents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateContentsRecordTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClass(helloInt.class));

    @Inject
    Engine engine;

    @Test
    public void testTemplateContents() throws InterruptedException, ExecutionException {
        // Clear preloaded templates to simulate quarkus.qute.dev-mode.no-restart-templates in the dev mode
        engine.clearTemplates();

        assertEquals("Hello 42!", new helloInt(42).render());
        assertEquals("Hello 1!", engine.getTemplate("TemplateContentsRecordTest/helloInt").data("val", 1).render());
        assertEquals("Hello 42!", new helloLong(42).render());
        assertEquals("Hello 1!", engine.getTemplate("foo/helloLong").data("val", 1).render());

        TemplateInstance helloHtml = new helloHtml("\"Martin\"");
        assertNotNull(helloHtml.getTemplate().getVariant());
        assertEquals(Variant.TEXT_HTML, helloHtml.getTemplate().getVariant().get().getContentType());
        assertEquals("<p>Hello &quot;Martin&quot;!</p>", helloHtml.render());
        assertEquals("<p>Hello Lu!</p>",
                engine.getTemplate("TemplateContentsRecordTest/helloHtml.html").data("name", "Lu").render());
    }

    @TemplateContents("Hello {val}!")
    record helloInt(int val) implements TemplateInstance {
    }

    @CheckedTemplate(basePath = "foo")
    @TemplateContents("Hello {val}!")
    record helloLong(long val) implements TemplateInstance {
    }

    @TemplateContents(value = "<p>Hello {name}!</p>", suffix = "html")
    record helloHtml(String name) implements TemplateInstance {
    }

}
