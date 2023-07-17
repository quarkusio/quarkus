package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                "PREFIX::{#fragment foo rendered=false}FOO{/fragment}::{#include $foo /}::{#include $foo /}", null, "foo");
        assertEquals("PREFIX::::FOO::FOO", foo.render());
        assertEquals("FOO", foo.getFragment("foo").render());
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

}
