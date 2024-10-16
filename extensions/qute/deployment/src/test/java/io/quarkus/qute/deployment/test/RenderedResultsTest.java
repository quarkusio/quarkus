package io.quarkus.qute.deployment.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.RenderedResults;
import io.quarkus.qute.RenderedResults.RenderedResult;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.Variant;
import io.quarkus.test.QuarkusUnitTest;

public class RenderedResultsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addClasses(SimpleBean.class, FooTemplates.class)
                    .addAsResource(new StringAsset("quarkus.qute.suffixes=txt,html"), "application.properties")
                    .addAsResource(new StringAsset("{name}{#fragment id=bar rendered=false}bar{/fragment}"),
                            "templates/foo.txt")
                    .addAsResource(new StringAsset("<h1>{name}{#fragment id=bar rendered=false}bar{/fragment}</h1>"),
                            "templates/foo.html"));

    @Inject
    RenderedResults renderedResults;

    @Inject
    SimpleBean bean;

    @Test
    public void testInjectedTemplate() throws InterruptedException {
        assertResults(() -> bean.fooInstance().data("name", "oof").render(), "foo.txt", "oof");
    }

    @Test
    public void testInjectedTemplateSelectedVariant() throws InterruptedException {
        assertResults(() -> bean.fooInstance()
                .setAttribute(TemplateInstance.SELECTED_VARIANT, Variant.forContentType(Variant.TEXT_HTML))
                .data("name", "oof")
                .render(), "foo.html", "<h1>oof</h1>");
    }

    @Test
    public void testTypesafeTemplate() throws InterruptedException {
        assertResults(() -> FooTemplates.foo("oof").render(), "foo.txt", "oof");
    }

    @Test
    public void testTypesafeFragment() throws InterruptedException {
        assertResults(() -> FooTemplates.foo$bar().render(), "foo.txt$bar", "bar");
    }

    @Test
    public void testTypesafeTemplateSelectedVariant() throws InterruptedException {
        assertResults(
                () -> FooTemplates.foo("oof")
                        .setAttribute(TemplateInstance.SELECTED_VARIANT, Variant.forContentType(Variant.TEXT_HTML)).render(),
                "foo.html", "<h1>oof</h1>");
    }

    @Test
    public void testTypesafeFragmentSelectedVariant() throws InterruptedException {
        assertResults(
                () -> FooTemplates.foo$bar()
                        .setAttribute(TemplateInstance.SELECTED_VARIANT, Variant.forContentType(Variant.TEXT_HTML)).render(),
                "foo.html$bar", "bar");
    }

    private void assertResults(Supplier<String> renderAction, String templateId, String expectedResult)
            throws InterruptedException {
        renderedResults.clear();
        assertEquals(expectedResult, renderAction.get());
        // Wait a little so that we can test the RenderedResult#timeout()
        // Note that LocalDateTime.now() has precision of the system clock and it seems that windows has millisecond precision
        TimeUnit.MILLISECONDS.sleep(50);
        List<RenderedResult> results = renderedResults.getResults(templateId);
        assertEquals(1, results.size(), renderedResults.toString());
        assertEquals(expectedResult, results.get(0).result());
        assertEquals(expectedResult, renderAction.get());
        results = renderedResults.getResults(templateId);
        assertEquals(2, results.size(), renderedResults.toString());
        assertEquals(expectedResult, results.get(1).result());
        assertTrue(results.get(0).timestamp().isBefore(results.get(1).timestamp()));
        renderedResults.clear();
        assertTrue(renderedResults.getResults(templateId).isEmpty());
    }

}
