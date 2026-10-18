package io.quarkus.qute.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Expression;
import io.quarkus.qute.Template;
import io.quarkus.qute.deployment.TemplatesAnalysisBuildItem.TemplateAnalysis;
import io.quarkus.qute.runtime.QuteConfig;

public class QuteProcessorTest {

    @Test
    public void testTemplateDataIgnorePattern() throws Exception {
        List<String> names = List.of("foo", "bar");
        Pattern p = Pattern.compile(QuteProcessor.buildIgnorePattern(names));
        // Ignore "baz" and "getFoo"
        assertTrue(p.matcher("baz").matches());
        assertTrue(p.matcher("getFoo").matches());
        // Do not ignore "foo" and "bar"
        for (String name : names) {
            assertFalse(p.matcher(name).matches());
        }
        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> QuteProcessor.buildIgnorePattern(List.of()));
    }

    @Test
    public void testCollectNamespaceExpressions() throws Exception {
        Template template = Engine.builder().build().parse("{msg:hello} {msg2:hello_alpha} {foo:baz.get(foo:bar)}");
        TemplateAnalysis analysis = new TemplateAnalysis("foo", template, null);
        Set<Expression> msg = QuteProcessor.collectNamespaceExpressions(analysis, "msg");
        assertEquals(1, msg.size());
        assertEquals("msg:hello", msg.iterator().next().toOriginalString());

        Set<Expression> msg2 = QuteProcessor.collectNamespaceExpressions(analysis, "msg2");
        assertEquals(1, msg2.size());
        assertEquals("msg2:hello_alpha", msg2.iterator().next().toOriginalString());

        Set<Expression> foo = QuteProcessor.collectNamespaceExpressions(analysis, "foo");
        assertEquals(2, foo.size());
        for (Expression fooExpr : foo) {
            assertTrue(
                    fooExpr.toOriginalString().equals("foo:bar") || fooExpr.toOriginalString().equals("foo:baz.get(foo:bar)"));
        }
    }

    @Test
    public void testCollectTemplateRoots() {
        TemplateRootsBuildItem roots = new QuteProcessor().collectTemplateRoots(List.of(
                new TemplateRootBuildItem("templates"),
                new TemplateRootBuildItem("/emails/"),
                new TemplateRootBuildItem("templates")));

        assertEquals(Set.of("templates", "emails"), roots.getPaths());
    }

    @Test
    public void testCollectTemplateVariants() throws IOException {
        EffectiveTemplatePathsBuildItem paths = new EffectiveTemplatePathsBuildItem(List.of(
                templatePath("item.html"), templatePath("item.txt"), templatePath("detail.html"),
                templatePath("ignored.json")));

        Map<String, List<String>> variants = new QuteProcessor()
                .collectTemplateVariants(paths, configWithSuffixes("html", "txt")).getVariants();

        assertThat(variants).containsOnlyKeys("item", "detail");
        assertThat(variants.get("item")).containsExactlyInAnyOrder("item.html", "item.txt");
        assertThat(variants.get("detail")).containsExactly("detail.html");
    }

    private static TemplatePathBuildItem templatePath(String path) {
        return TemplatePathBuildItem.builder().path(path).extensionInfo("test").build();
    }

    private static QuteConfig configWithSuffixes(String... suffixes) {
        return (QuteConfig) Proxy.newProxyInstance(QuteConfig.class.getClassLoader(), new Class<?>[] { QuteConfig.class },
                (proxy, method, args) -> method.getName().equals("suffixes") ? List.of(suffixes) : null);
    }

}
