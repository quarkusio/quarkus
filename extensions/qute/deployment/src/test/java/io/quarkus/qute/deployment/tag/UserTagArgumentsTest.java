package io.quarkus.qute.deployment.tag;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Engine;
import io.quarkus.qute.RawString;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.UserTagSectionHelper;
import io.quarkus.test.QuarkusUnitTest;

public class UserTagArgumentsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(
                            "{_args.size}::{_args.empty}::{_args.get('name')}::{_args.asHtmlAttributes}::{_args.skip('foo','baz').size}::{#each _args.filter('name')}{it.value}{/each}"),
                            "templates/tags/hello.txt")
                    .addAsResource(new StringAsset("{#hello name=val /}"), "templates/foo.txt")
                    .addAsResource(new StringAsset("{_args.startsWith('hx-','x-')}"),
                            "templates/tags/startsWith.txt"));

    @Inject
    Template foo;

    @Inject
    Engine engine;

    @Test
    public void testInjection() {
        assertEquals("1::false::Lu::name=\"Lu\"::1::Lu", foo.data("val", "Lu").render());
    }

    @Test
    public void testNotBuiltInTemplateExtension() {
        assertEquals("hx-get=\"/endpoint\" x-data=\"{}\"",
                engine.parse("{#startsWith hx-get='/endpoint' x-data='{}' ignored-arg='ignored' /}").render());
    }

    @TemplateExtension
    public static class Extensions {

        public static RawString startsWith(UserTagSectionHelper.Arguments obj, String... startsWithPrefix) {
            Set<String> startsWithSet = Set.of(startsWithPrefix);
            Predicate<Map.Entry<String, Object>> predicate = e -> startsWithSet.stream()
                    .anyMatch(k -> e.getKey().startsWith(k));
            return obj.filter(predicate).asHtmlAttributes();
        }

    }

}
