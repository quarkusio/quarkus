package io.quarkus.qute.deployment.contents;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Engine;
import io.quarkus.qute.TemplateContents;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateContentsCheckedTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root.addClass(Templates.class));

    @Inject
    Engine engine;

    @Test
    public void testTemplateContents() throws InterruptedException, ExecutionException {
        assertEquals("Hello 42!", Templates.helloInt(42).render());
        assertEquals("Hello 1!", engine.getTemplate("TemplateContentsCheckedTest/helloInt").data("val", 1).render());
    }

    @CheckedTemplate
    static class Templates {

        @TemplateContents("Hello {val}!")
        static native TemplateInstance helloInt(int val);

    }

}
