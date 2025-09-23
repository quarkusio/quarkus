package io.quarkus.qute.deployment.altexprcommand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.qute.Template;
import io.quarkus.qute.deployment.TemplateRootBuildItem;
import io.quarkus.test.QuarkusUnitTest;

public class AlternativeExprSyntaxDisabledCustomRootTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(root -> root
                    .addAsResource(new StringAsset("{=foo.toLowerCase}{ignored}"), "templates/testik.html")
                    .addAsResource(new StringAsset("alt-expr-syntax=false"), "web/public/.qute")
                    .addAsResource(new StringAsset("{=foo.toLowerCase}{ignored}"), "web/public/testik2.html"))
            .overrideConfigKey("quarkus.qute.alt-expr-syntax", "true")
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new TemplateRootBuildItem("web/public"));
                    }
                }).produces(TemplateRootBuildItem.class)
                        .build();
            }
        };
    }

    @Inject
    Template testik;

    @Inject
    Template testik2;

    @Test
    public void testAltSyntax() {
        assertEquals("clement{ignored}", testik.data("foo", "Clement").render());
        assertEquals("andy{ignored}", testik2.data("foo", "Andy").render());
    }

}
