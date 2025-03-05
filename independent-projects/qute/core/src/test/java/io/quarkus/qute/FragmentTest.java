package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.Template.Fragment;

public class FragmentTest {

    @Test
    public void testSimpleFragment() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine
                .parse("PREFIX {#fragment id='foo_and_bar'}{foo}{/} {#fragment another}{foo}{/}SUFFIX",
                        Variant.forContentType(Variant.TEXT_PLAIN), "fragments.html");
        assertEquals("OK", template.getFragment("foo_and_bar").data("foo", "OK").render());
        assertEquals("NOK", template.getFragment("another").data("foo", "NOK").render());
        assertFalse(template.isFragment());
        Fragment another = template.getFragment("another");
        assertTrue(another.isFragment());
        assertEquals("another", another.getId());
        assertEquals(template.getFragment("another").getGeneratedId(), another.getGeneratedId());
        assertEquals("fragments.html", template.getFragment("another").getOriginalTemplate().getId());
        assertEquals(Set.of("foo_and_bar", "another"), template.getFragmentIds());
        List<TemplateNode> anotherNodes = another.getNodes();
        assertEquals(1, anotherNodes.size());
        assertTrue(anotherNodes.get(0).isExpression());
    }

    @Test
    public void testNestedFragment() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine
                .parse("PREFIX {#fragment id=foo_and_bar}{foo}{#fragment another}{foo}{/}{/} SUFFIX",
                        Variant.forContentType(Variant.TEXT_PLAIN), "fragments.html");
        assertEquals("OKOK", template.getFragment("foo_and_bar").data("foo", "OK").render());
        assertEquals("NOK", template.getFragment("another").data("foo", "NOK").render());
        assertEquals("NOK", template.getFragment("foo_and_bar").getFragment("another").data("foo", "NOK").render());
        assertEquals("NOKNOK", template.getFragment("foo_and_bar").getFragment("another").getFragment("foo_and_bar")
                .data("foo", "NOK").render());
    }

    @Test
    public void testNonUniqueIds() {
        Engine engine = Engine.builder().addDefaults().build();
        TemplateException expected = assertThrows(TemplateException.class,
                () -> engine.parse("{#fragment id=another}{foo}{/}{#fragment another}{foo}{/}", null, "bum.html"));
        assertEquals(FragmentSectionHelper.Code.NON_UNIQUE_FRAGMENT_ID, expected.getCode());
        assertEquals("Parser error in template [bum.html] line 1: found a non-unique fragment identifier: [another]",
                expected.getMessage());
    }

    @Test
    public void testInvisibleFragment() {
        Engine engine = Engine.builder().addDefaults().build();
        Template foo = engine.parse(
                "PREFIX::{#fragment foo _hidden}FOO{/fragment}::{#include $foo /}::{#include $foo /}", null, "foo");
        assertEquals("PREFIX::::FOO::FOO", foo.render());
        assertEquals("FOO", foo.getFragment("foo").render());
    }

    @Test
    public void testFrgNamespace() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addNamespaceResolver(new FragmentNamespaceResolver())
                .addValueResolver(new ReflectionValueResolver())
                .build();
        Template foo = engine.parse(
                "PREFIX::{#fragment foo rendered=false}FOO{/fragment}::{frg:foo.toLowerCase}::{#include $foo /}", null, "foo");
        assertEquals("PREFIX::::foo::FOO", foo.render());
        // Fragment from another template
        engine.putTemplate("bar", engine.parse("""
                {#fragment barbar _hidden}
                Barbar is here!
                {/}
                """));
        assertEquals("Barbar is here!", engine.parse("{frg:bar$barbar}").render().strip());
        assertThrows(TemplateException.class, () -> engine.parse("{frg:nonexistent$barbar}").render());
    }

    @Test
    public void testCapture() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addNamespaceResolver(new FragmentNamespaceResolver(FragmentNamespaceResolver.CAP))
                .addValueResolver(new ReflectionValueResolver())
                .build();
        Template foo = engine.parse(
                "PREFIX::{#capture foo}FOO{/capture}::{cap:foo.toLowerCase}::{#include $foo /}", null, "foo");
        assertEquals("PREFIX::::foo::FOO", foo.render());
    }

    @Test
    public void testCaptureArgs() {
        Engine engine = Engine.builder()
                .addDefaults()
                .addNamespaceResolver(new FragmentNamespaceResolver(FragmentNamespaceResolver.CAPTURE))
                .addNamespaceResolver(new NamedArgument.ParamNamespaceResolver())
                .addValueResolver(new NamedArgument.SetValueResolver())
                .addValueResolver(new ReflectionValueResolver())
                .build();
        Template foo = engine.parse(
                "PREFIX::{#capture foo}{name} {surname}{/capture}::{capture:foo(param:name = 'Ondik',param:surname.set(mySurname)).toLowerCase}",
                null, "foo");
        assertEquals("PREFIX::::ondik kouba", foo.data("mySurname", "Kouba").render());
    }

    @Test
    public void testInvalidId() {
        Engine engine = Engine.builder().addDefaults().build();
        TemplateException expected = assertThrows(TemplateException.class,
                () -> engine.parse("{#fragment id='another and foo'}{/}", null, "bum.html"));
        assertEquals(FragmentSectionHelper.Code.INVALID_FRAGMENT_ID, expected.getCode());
        assertEquals(
                "Parser error in template [bum.html] line 1: found an invalid fragment identifier [another and foo] - an identifier can only consist of alphanumeric characters and underscores",
                expected.getMessage());
    }

    @Test
    public void testNestedFragmentRendered() {
        Engine engine = Engine.builder().addDefaults().build();
        Template alpha = engine.parse("""
                    OK
                    {#fragment id=\"nested\" rendered=false}
                    NOK
                    {/}
                    {#fragment id=\"visible\"}
                    01
                    {/fragment}
                """);
        engine.putTemplate("alpha", alpha);
        assertEquals("OK01", alpha.render().replaceAll("\\s", ""));
        assertEquals("NOK", alpha.getFragment("nested").render().trim());

        Template bravo = engine.parse("""
                {#include $nested}
                {#fragment id=\"nested\" rendered=false}
                OK
                {/}
                """);
        assertEquals("OK", bravo.render().trim());
        assertEquals("OK", bravo.getFragment("nested").render().trim());

        assertEquals("NOK", engine.parse("{#include alpha$nested /}").render().trim());
        Template charlie = engine.parse("{#include alpha /}");
        assertEquals("OK01", charlie.render().replaceAll("\\s", ""));

        Template delta = engine.parse("""
                {#fragment id=\"nested\" rendered=false}
                    {#include alpha /}
                {/}
                """);
        assertEquals("OK01", delta.getFragment("nested").render().replaceAll("\\s", ""));
    }

}
