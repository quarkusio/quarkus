package io.quarkus.qute.deployment.extensions;

import static io.quarkus.qute.TemplateExtension.ANY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.deployment.Foo;
import io.quarkus.test.QuarkusUnitTest;

public class TemplateExtensionMethodsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(Foo.class, Extensions.class, PrioritizedExtensions.class)
                    .addAsResource(new StringAsset("{foo.name.toLower} {foo.name.ignored} {foo.callMe(1)} {foo.baz}"),
                            "templates/foo.txt")
                    .addAsResource(new StringAsset("{baz.setScale(baz.defaultScale,roundingMode)}"),
                            "templates/baz.txt")
                    .addAsResource(new StringAsset("{anyInt.foo('bing')}"),
                            "templates/any.txt")
                    .addAsResource(new StringAsset("{foo.pong}::{foo.name}"),
                            "templates/priority.txt"));

    @Inject
    Template foo;

    @Inject
    Engine engine;

    @Test
    public void testTemplateExtensions() {
        assertEquals("fantomas NOT_FOUND 11 baz",
                foo.data("foo", new Foo("Fantomas", 10l)).render());
    }

    @Test
    public void testMethodParameters() {
        assertEquals("123.46",
                engine.getTemplate("baz.txt").data("roundingMode", RoundingMode.HALF_UP).data("baz", new BigDecimal("123.4563"))
                        .render());
    }

    @Test
    public void testMatchAnyWithParameter() {
        assertEquals("10=bing",
                engine.getTemplate("any.txt").data("anyInt", 10).render());
    }

    @Test
    public void testBuiltinExtensions() {
        Map<String, String> map = new HashMap<String, String>();
        map.put("alpha", "1");
        map.put("bravo", "2");
        map.put("charlie", "3");
        assertEquals("3:1:NOT_FOUND:1:false:true",
                engine.parse(
                        "{myMap.size}:{myMap.alpha}:{myMap.missing}:{myMap.get(key)}:{myMap.empty}:{myMap.containsKey('charlie')}")
                        .data("myMap", map).data("key", "alpha").render());

    }

    @Test
    public void testPriority() {
        assertEquals("bravo::baz", engine.getTemplate("priority").data("foo", new Foo("baz", 10l)).render());
    }

    @Test
    public void testListGetByIndex() {
        assertEquals("true=true=NOT_FOUND",
                engine.parse("{list.0}={list[0]}={list[100]}").data("list", Collections.singletonList(true)).render());
    }

    @TemplateExtension
    public static class Extensions {

        String ignored(String val) {
            return val.toLowerCase();
        }

        static String toLower(String val) {
            return val.toLowerCase();
        }

        static Long callMe(Foo foo, Integer val) {
            return foo.age + val;
        }

        @TemplateExtension(matchName = "baz")
        static String override(Foo foo) {
            return "baz";
        }

        static BigDecimal setScale(BigDecimal val, int scale, RoundingMode mode) {
            return val.setScale(scale, mode);
        }

        @TemplateExtension(matchName = ANY)
        static String any(Integer val, String name, String info) {
            return val + "=" + info;
        }

        static int defaultScale(BigDecimal val) {
            return 2;
        }
    }

    @TemplateExtension(matchName = "pong")
    public static class PrioritizedExtensions {

        // default priority - class-level annotation
        static String alpha(Foo foo) {
            return "alpha";
        }

        // Explicit priority, higher than ValueResolverGenerator.DEFAULT_PRIORITY
        @TemplateExtension(matchName = "pong", priority = 100)
        static String bravo(Foo foo) {
            return "bravo";
        }

        // default priority - method-level annotation
        @TemplateExtension(matchName = "pong")
        static String charlie(Foo foo) {
            return "charlie";
        }

    }

}
