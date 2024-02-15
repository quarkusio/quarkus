package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class UserTagTest {

    @Test
    public void testUserTag() {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers()
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .build();

        Template tag = engine.parse("{#if showImage.or(false)}{it.name}{#else}nope{/if}");
        // showImage.or(false), it.name
        assertEquals(2, tag.getExpressions().size());
        engine.putTemplate("my-tag-id", tag);

        Map<String, Object> order = new HashMap<>();
        order.put("name", "Herbert");
        assertEquals("Herbert",
                engine.parse("{#myTag order showImage=true /}").render(Collections.singletonMap("order", order)));
        assertEquals("nope", engine.parse("{#myTag order /}").render(Collections.singletonMap("order", order)));
        assertEquals("nope", engine.parse("{#myTag showImage=false /}").render(Collections.singletonMap("order", order)));
        assertEquals("nope",
                engine.parse("{#each this}{#myTag showImage=false /}{/each}").render(Collections.singletonMap("order", order)));
        assertEquals("Herbert",
                engine.parse("{#each this}{#myTag it showImage=true _isolated=false /}{/each}")
                        .render(Collections.singletonList(order)));
    }

    @Test
    public void testUserTagWithNestedContent() {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers()
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .build();

        Template tag = engine.parse("{#if showImage.or(false)}<b>{#insert /}</b>{#else}nope{/if}");
        engine.putTemplate("my-tag-id", tag);

        Map<String, Object> order = new HashMap<>();
        order.put("name", "Herbert");
        assertEquals("<b>Herbert</b>",
                engine.parse("{#myTag showImage=true _isolated=false}{order.name}{/}")
                        .render(Collections.singletonMap("order", order)));
        assertEquals("nope", engine.parse("{#myTag}{order.name}{/}").render(Collections.singletonMap("order", order)));
        assertEquals("nope",
                engine.parse("{#myTag showImage=false}{order.name}{/}").render(Collections.singletonMap("order", order)));
        assertEquals("nope",
                engine.parse("{#each this}{#myTag showImage=false}{it.name}{/}{/each}")
                        .render(Collections.singletonMap("order", order)));
        assertEquals("<b>Herbert</b>",
                engine.parse("{#each this}{#myTag showImage=true _isolated=false}{it.name}{/}{/each}")
                        .render(Collections.singletonList(order)));
    }

    @Test
    public void testUserTagWithRichNestedContent() {
        Engine engine = Engine.builder().addDefaultSectionHelpers().addDefaultValueResolvers()
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag2", "my-tag-id2"))
                .build();

        Template tag = engine.parse("{#if showImage}<b>{nested-content}</b>{#else}nope{/if}");
        engine.putTemplate("my-tag-id", tag);

        Template tag2 = engine.parse("{#if showImage2}<i>{nested-content}</i>{#else}nope2{/if}");
        engine.putTemplate("my-tag-id2", tag2);

        Map<String, Object> order = new HashMap<>();
        order.put("name", "Herbert");
        assertEquals("<b><i>Herbert</i></b>",
                engine.parse("{#myTag showImage=true}{#myTag2 showImage2=true}{order.name}{/myTag2}{/myTag}")
                        .render(Collections.singletonMap("order", order)));
    }

    @Test
    public void testUserTagLoopParam() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .build();

        Template tag = engine.parse("{it} {surname}");
        engine.putTemplate("my-tag-id", tag);

        assertEquals("KOUBA kouba",
                engine.parse("{#each surnames}{#myTag it.toUpperCase surname=it.toLowerCase /}{/each}")
                        .data("surnames", Collections.singleton("Kouba")).render());
        assertEquals("KOUBA kouba",
                engine.parse(
                        "{#for surname in surnames}{#each surnames}{#myTag it.toUpperCase surname=surname.toLowerCase /}{/each}{/for}")
                        .data("surnames", Collections.singleton("Kouba")).render());
    }

    @Test
    public void testEval() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("itemDetail", "my-tag-id"))
                .build();

        Template tag = engine.parse("{#set item=items.get(itemId)}{#eval myNestedContent item=item /}{/set}");
        engine.putTemplate("my-tag-id", tag);

        assertEquals("10 kg",
                engine.parse("{#itemDetail itemId=1 myNestedContent=\"{item.quantity} {item.unit}\" _isolated=false /}")
                        .data("items", Map.of(1, Map.of("quantity", 10, "unit", "kg"))).render());
    }

    @Test
    public void testDefaultedKey() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .strictRendering(false)
                .build();

        Template tag = engine.parse("{it}:{name}:{isCool}:{age}:{foo.bar}:{foo}");
        engine.putTemplate("my-tag-id", tag);
        assertEquals("Ondrej:Ondrej:true:2:NOT_FOUND:NOT_FOUND",
                engine.parse("{#myTag name age=2 isCool foo.length _isolated=true/}")
                        .data("name", "Ondrej", "isCool", true, "foo", "bzzz").render());
        assertEquals("Ondrej:Ondrej:true:2:NOT_FOUND:NOT_FOUND",
                engine.parse("{#myTag name age=2 isCool foo.length _isolated /}")
                        .data("name", "Ondrej", "isCool", true, "foo", "bzzz").render());
    }

    @Test
    public void testInsertSections() {
        Engine engine = Engine.builder().addDefaults().addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag1", "mytag1"))
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag2", "mytag2"))
                .build();
        engine.putTemplate("mytag1", engine.parse("{#insert foo}No foo!{/insert}::{#insert bar}No bar!{/insert}"));
        engine.putTemplate("mytag2", engine.parse("{#insert}Default content{/insert}"));

        assertEquals("Baz!::No bar!", engine.parse("{#myTag1 name='Baz'}{#foo}{name}!{/foo}{/myTag1}").render());
        assertEquals("Baz!", engine.parse("{#myTag2}Baz!{/myTag2}").render());
    }

    @Test
    public void testIsolation() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag", "my-tag-id"))
                .strictRendering(false)
                .build();

        Template tag = engine.parse("{name}");
        engine.putTemplate("my-tag-id", tag);
        assertEquals("NOT_FOUND", engine.parse("{#myTag /}").data("name", "Dorka").render());
        assertEquals("Dorka", engine.parse("{#myTag _isolated=false /}").data("name", "Dorka").render());
        assertEquals("Dorka", engine.parse("{#myTag _unisolated /}").data("name", "Dorka").render());
    }

    @Test
    public void testArguments() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag1", "my-tag-1"))
                .addSectionHelper(new UserTagSectionHelper.Factory("myTag2", "my-tag-2"))
                .addSectionHelper(new UserTagSectionHelper.Factory("gravatar", "gravatar-tag"))
                .addResultMapper(new HtmlEscaper(ImmutableList.of("text/html")))
                .strictRendering(false)
                .build();
        Template tag1 = engine.parse(
                "{_args.size}::{_args.empty}::{_args.get('foo').or('bar')}::{_args.asHtmlAttributes}::{_args.skip('foo','baz').size}::{#each _args.filter('foo')}{it.value}{/each}",
                Variant.forContentType(Variant.TEXT_HTML));
        engine.putTemplate("my-tag-1", tag1);
        Template tag2 = engine.parse(
                "{#each _args}{it.key}=\"{it.value}\"{#if it_hasNext} {/if}{/each}");
        engine.putTemplate("my-tag-2", tag2);
        engine.putTemplate("gravatar-tag", engine.parse(
                "<img src=\"https://www.gravatar.com/avatar/{hash}{#if size}?s={size}{/if}\" {_args.skip('hash','size').asHtmlAttributes}/>"));

        Template template = engine.parse("{#myTag1 /}");
        assertEquals("0::true::bar::::0::", template.render());
        assertEquals("3::false::1::bar=\"true\" baz=\"&quot;\" foo=\"1\"::1::1",
                engine.parse("{#myTag1 foo=1 bar=true baz=quotationMark /}").data("quotationMark", "\"").render());

        assertEquals("baz=\"false\" foo=\"1\"", engine.parse("{#myTag2 foo=1 baz=false /}").render());

        assertEquals(
                "<img src=\"https://www.gravatar.com/avatar/ia3andy\" alt=\"ia3andy\" class=\"rounded\" title=\"https://github.com/ia3andy\"/>",
                engine.parse("{#gravatar hash='ia3andy' alt='ia3andy' title='https://github.com/ia3andy' class='rounded' /}")
                        .render());
    }

    @Test
    public void testArgumentsAsHtmlAttributes() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("arg", "arg-tag"))
                .addResultMapper(new HtmlEscaper(ImmutableList.of("text/html")))
                .strictRendering(true)
                .build();
        engine.putTemplate("arg-tag", engine.parse("{_args.asHtmlAttributes}"));
        // Assert that "it" is always skipped; ['foo'] becomes [it='foo'] and is also registered as [foo='foo']; and ['readonly'] becomes [readonly='readonly']
        assertEquals("class=\"rounded\" foo=\"foo\" hash=\"ia3andy\" readonly=\"readonly\"",
                engine.parse("{#arg 'foo' hash='ia3andy' class='rounded' 'readonly' /}").render());
    }

    @Test
    public void testArgumentsIdenticalKeyValue() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("arg", "arg-tag"))
                .addResultMapper(new HtmlEscaper(ImmutableList.of("text/html")))
                .strictRendering(true)
                .build();
        engine.putTemplate("arg-tag", engine.parse("{_args.skipIdenticalKeyValue.size}"));
        assertEquals("1",
                engine.parse("{#arg 'foo' hash='ia3andy' 'readonly' /}").render());
        engine.putTemplate("arg-tag", engine.parse("{_args.filterIdenticalKeyValue.size}"));
        assertEquals("2",
                engine.parse("{#arg 'foo' hash='ia3andy' 'readonly' /}").render());
    }

    @Test
    public void testSkipIt() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addValueResolver(new ReflectionValueResolver())
                .addSectionHelper(new UserTagSectionHelper.Factory("arg", "arg-tag"))
                .addResultMapper(new HtmlEscaper(ImmutableList.of("text/html")))
                .strictRendering(true)
                .build();
        engine.putTemplate("arg-tag", engine.parse("{_args.skipIt.asHtmlAttributes}"));
        assertEquals("class=\"rounded\" hash=\"ia3andy\" readonly=\"readonly\"",
                engine.parse("{#arg 'foo' hash='ia3andy' class='rounded' 'readonly' /}").render());
        assertEquals("class=\"rounded\" hash=\"ia3andy\" readonly=\"readonly\"",
                engine.parse("{#arg hash='ia3andy' class='rounded' 'readonly' /}").render());
        assertEquals("",
                engine.parse("{#arg names.size /}").data("names", List.of()).render());
        assertEquals("foo=\"true\"",
                engine.parse("{#arg foo=true 'foo and bar' /}").render());
    }
}
