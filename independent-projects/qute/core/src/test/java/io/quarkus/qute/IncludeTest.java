package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class IncludeTest {

    @Test
    public void testInclude() {
        Engine engine = Engine.builder().addSectionHelper(new IncludeSectionHelper.Factory())
                .addSectionHelper(new InsertSectionHelper.Factory())
                .addValueResolver(ValueResolvers.thisResolver())
                .build();

        engine.putTemplate("super", engine.parse("{this}: {#insert header}default header{/insert}"));
        assertEquals("HEADER: super header",
                engine.parse("{#include super}{#header}super header{/header}{/include}").render("HEADER"));
    }

    @Test
    public void testMultipleInserts() {
        Engine engine = Engine.builder().addSectionHelper(new IncludeSectionHelper.Factory())
                .addSectionHelper(new InsertSectionHelper.Factory())
                .addValueResolver(ValueResolvers.thisResolver())
                .build();

        engine.putTemplate("super",
                engine.parse("{#insert header}default header{/insert} AND {#insert content}default content{/insert}"));

        Template template = engine
                .parse("{#include super}{#header}super header{/header}  {#content}super content{/content} {/include}");
        assertEquals("super header AND super content", template.render(null));
    }

    @Test
    public void testIncludeSimpleData() {
        Engine engine = Engine.builder().addSectionHelper(new IncludeSectionHelper.Factory())
                .addSectionHelper(new InsertSectionHelper.Factory())
                .addValueResolver(ValueResolvers.mapResolver())
                .build();

        Map<String, String> data = new HashMap<>();
        data.put("name", "Al");
        data.put("price", "100");
        engine.putTemplate("detail", engine.parse("<strong>{name}</strong>:{price}"));
        assertEquals("<strong>Al</strong>:100",
                engine.parse("{#include detail/}").render(data));
    }

    @Test
    public void testOptionalBlockEndTags() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("super", engine.parse("{#insert header}header{/}:{#insert footer /}"));
        assertEquals("super header:super footer",
                engine.parse("{#include super}{#header}super header{#footer}super footer{/include}").render());
    }

    @Test
    public void testIncludeInLoop() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo", engine.parse("{#insert snippet}empty{/insert}"));
        assertEquals("1.2.3.4.5.",
                engine.parse("{#for i in 5}{#include foo}{#snippet}{count}.{/snippet} this should be ingored {/include}{/for}")
                        .render());
    }

    @Test
    public void testIncludeInIf() {
        Engine engine = Engine.builder().addDefaults().build();
        engine.putTemplate("foo", engine.parse("{#insert snippet}empty{/insert}"));
        assertEquals("1",
                engine.parse("{#if true}{#include foo} {#snippet}1{/snippet} {/include}{/if}")
                        .render());
    }

    @Test
    public void testUserTagInsideInsert() {
        Engine engine = Engine.builder().addDefaults().addSectionHelper(new UserTagSectionHelper.Factory("hello", "hello"))
                .build();
        engine.putTemplate("hello", engine.parse("{name}"));
        engine.putTemplate("base", engine.parse("{#insert snippet}{/insert}"));
        assertEquals("foo",
                engine.parse("{#include base} {#snippet}{#hello name='foo'/}{/snippet} {/include}")
                        .render());
    }

}
