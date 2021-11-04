package io.quarkus.qute.deployment.include;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;

public class IncludeTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset("{#insert item}NOK{/}:{#insert foo}default foo{/}"), "templates/base.html")
                    .addAsResource(new StringAsset("{#include base}{#item}OK{/}{#foo}my foo{/include}"),
                            "templates/detail.html")
                    .addAsResource(new StringAsset(
                            // The param declaration is needed to generate the value resolver correctly
                            "{@io.quarkus.qute.deployment.include.IncludeTest$TemplateContext context} Context data: {context.printData(\"Hello\")}\n"
                                    + ""),
                            "templates/include.html")
                    .addAsResource(new StringAsset("<body>\n"
                            + "Hello from test template\n"
                            + "<p>\n"
                            + "{#include include /}\n"
                            + "</body>"),
                            "templates/test.html"));

    @CheckedTemplate(basePath = "")
    static class Templates {
        static native TemplateInstance test(TemplateContext context);
    }

    @Inject
    Template detail;

    @Test
    public void testIncludeSection() {
        assertEquals("OK:my foo", detail.render());
    }

    @Test
    public void testCheckedTemplate() {
        assertEquals("<body>\n"
                + "Hello from test template\n"
                + "<p>\n"
                + " Context data: Hello\n"
                + "</body>", Templates.test(new TemplateContext()).render());
    }

    public static class TemplateContext {

        public String printData(String param) {
            return param;
        }
    }

}
