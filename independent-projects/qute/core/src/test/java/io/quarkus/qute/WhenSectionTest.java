package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class WhenSectionTest {

    @Test
    public void testWhen() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("{#when testVal}"
                + "{#is 'foo'}"
                + "Foo"
                + "{#is 'bar'}"
                + "Bar"
                + "{#else}"
                + "None"
                + "{/when}");
        assertEquals("Foo", template.data("testVal", "foo").render());
        assertEquals("Bar", template.data("testVal", "bar").render());
        assertEquals("None", template.data("testVal", 1).render());
    }

    @Test
    public void testWhenNumber() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("{#when numVal}"
                + "{#is in 1 7 9}"
                + "0:{numVal}"
                + "{#is > 10}"
                + "1:{numVal}"
                + "{#is < 10}"
                + "2:{numVal}"
                + "{/when}");
        assertEquals("0:7", template.data("numVal", 7).render());
        assertEquals("1:12", template.data("numVal", 12).render());
        assertEquals("2:3", template.data("numVal", 3).render());
    }

    @Test
    public void testSwitch() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template1 = engine.parse("{#switch testVal}{#case 1}1P{#case 2}2P{#else}0P{/switch}");
        assertEquals("2P", template1.data("testVal", 2).render());
        assertEquals("1P", template1.data("testVal", 1).render());
        assertEquals("0P", template1.data("testVal", 20).render());
    }

    @Test
    public void testSwitchEnum() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("{#switch state}{#case ON}1{#case OFF}0{#case BROKEN}-1{#else}none{/switch}");
        assertEquals("1", template.data("state", State.ON).render());
        assertEquals("0", template.data("state", State.OFF).render());
        assertEquals("-1", template.data("state", State.BROKEN).render());
        assertEquals("none", template.data("state", State.UNKNOWN).render());
        try {
            fail(template.data("state", null).render());
        } catch (TemplateException expected) {
            assertEquals("Entry \"ON\" not found in the data map in expression {ON} in template <<synthetic>> on line 0",
                    expected.getMessage());
        }
    }

    @Test
    public void testWhenEnum() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("{#when state}{#is in ON OFF}valid{#case BROKEN}invalid{#else}none{/when}");
        assertEquals("valid", template.data("state", State.ON).render());
        assertEquals("valid", template.data("state", State.OFF).render());
        assertEquals("invalid", template.data("state", State.BROKEN).render());
        assertEquals("none", template.data("state", State.UNKNOWN).render());
        try {
            fail(template.data("state", null).render());
        } catch (TemplateException expected) {
            assertEquals("Entry \"ON\" not found in the data map in expression {ON} in template <<synthetic>> on line 0",
                    expected.getMessage());
        }
    }

    @Test
    public void testWhenNot() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("{#when testVal}"
                + "{#is not 'foo'}"
                + "Not a Foo"
                + "{#else}"
                + "Foo"
                + "{/when}");
        assertEquals("Not a Foo", template.data("testVal", 1).render());
        assertEquals("Not a Foo", template.data("testVal", "fooo").render());
        assertEquals("Foo", template.data("testVal", "foo").render());
    }

    @Test
    public void testInNotIn() {
        Engine engine = Engine.builder().addDefaults().build();
        Template templateIn = engine.parse("{#when testVal}"
                + "{#is in 'foo' itemName 'bar'}"
                + "In!"
                + "{#else}"
                + "Not in!"
                + "{/when}");
        assertEquals("In!", templateIn.data("testVal", "foo").data("itemName", null).render());
        assertEquals("In!", templateIn.data("testVal", "baz").data("itemName", "baz").render());
        assertEquals("Not in!", templateIn.data("testVal", "never").data("itemName", null).render());
        Template templateNotIn = engine.parse("{#when testVal}"
                + "{#is !in 'foo' itemName 'bar'}"
                + "Not in!"
                + "{#else}"
                + "In!"
                + "{/when}");
        assertEquals("Not in!", templateNotIn.data("testVal", "foos").data("itemName", null).render());
        assertEquals("Not in!", templateNotIn.data("testVal", "bazz").data("itemName", "baz").render());
        assertEquals("In!", templateNotIn.data("testVal", "baz").data("itemName", "baz").render());
    }

    @Test
    public void testWhenNotFound() {
        Engine engine = Engine.builder().addDefaults().build();
        Template template = engine.parse("{#when testMe}"
                + "{#is not 'foo'}"
                + "Not a Foo"
                + "{#else}"
                + "Foo"
                + "{/when}");
        try {
            fail(template.render());
        } catch (TemplateException expected) {
            assertEquals("Entry \"testMe\" not found in the data map in expression {testMe} in template 1 on line 1",
                    expected.getMessage());
        }
    }

}
