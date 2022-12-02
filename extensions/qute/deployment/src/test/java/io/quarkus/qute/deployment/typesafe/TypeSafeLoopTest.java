package io.quarkus.qute.deployment.typesafe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import io.quarkus.qute.deployment.Foo;
import io.quarkus.qute.deployment.MyFooList;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Uni;

public class TypeSafeLoopTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Foo.class, MyFooList.class, Item.class, Extensions.class)
                    .addAsResource(new StringAsset("{@java.util.List<io.quarkus.qute.deployment.Foo> list}"
                            + "{@io.quarkus.qute.deployment.MyFooList fooList}"
                            + "{#for foo in list}"
                            + "{foo.name}={foo.age}={foo.charlie.name}"
                            + "{/}"
                            + "::"
                            + "{#for foo in fooList}"
                            + "{foo.name}={foo.age}={foo.charlie.name}"
                            + "{/}"
                            + "::"
                            + "{fooList.get(0).name}"
                            + "::"
                            + "{#for item in items}{#each item.tags('foo')}{it}{/each}{/for}"
                            + "::"
                            + "{fooList.get(0).tags.size}={#each fooList.get(0).tags}{it}{/each}"), "templates/foo.html")
                    .addAsResource(new StringAsset("{@java.util.List<io.quarkus.qute.deployment.Foo> list}"
                            + "{#for foo in list}"
                            + "{#let name=foo.name}"
                            + "{#for char in name.toCharArray}"
                            + "{char}:"
                            + "{/for}"
                            + "{/let}"
                            + "{/for}"), "templates/nested.html"));

    @Inject
    Template foo;

    @Inject
    Template nested;

    @Test
    public void testValidation() {
        List<Foo> foos = Collections.singletonList(new Foo("bravo", 10l));
        MyFooList myFoos = new MyFooList(new Foo("alpha", 1l));
        List<Item> items = Arrays.asList(new Item());
        assertEquals("bravo=10=BRAVO::alpha=1=ALPHA::alpha::foobar::2=boxunbox",
                foo.data("list", foos, "fooList", myFoos, "items", items).render());
    }

    @Test
    public void testNestedHintsValidation() {
        List<Foo> foos = Collections.singletonList(new Foo("boom", 10l));
        assertEquals("b:o:o:m:",
                nested.data("list", foos).render());
    }

    static class Item {

    }

    @TemplateExtension
    static class Extensions {

        static List<String> tags(Item item, String foo) {
            return Arrays.asList(foo, "bar");
        }

        static Uni<List<String>> tags(Foo foo) {
            return Uni.createFrom().item(Arrays.asList("box", "unbox"));
        }

    }

}
