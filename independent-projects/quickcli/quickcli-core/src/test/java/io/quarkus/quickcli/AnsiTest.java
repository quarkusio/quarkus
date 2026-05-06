package io.quarkus.quickcli;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AnsiTest {

    @Test
    void offDisabled() {
        assertFalse(Ansi.OFF.enabled());
    }

    @Test
    void onEnabled() {
        assertTrue(Ansi.ON.enabled());
    }

    @Test
    void textStripsMarkup() {
        Ansi.Text text = new Ansi.Text("@|bold Hello|@ world", null);
        assertEquals("Hello world", text.toString());
    }

    @Test
    void textNoMarkup() {
        Ansi.Text text = new Ansi.Text("plain text", null);
        assertEquals("plain text", text.toString());
    }

    @Test
    void textNull() {
        Ansi.Text text = new Ansi.Text(null, null);
        assertEquals("", text.toString());
    }

    @Test
    void stripAnsiMarkupMultiple() {
        assertEquals("a and b",
                Ansi.Text.stripAnsiMarkup("@|red a|@ and @|green b|@"));
    }

    @Test
    void stripAnsiMarkupNested() {
        // No nested markup support — inner @ signs are literal
        assertEquals("plain", Ansi.Text.stripAnsiMarkup("plain"));
    }

    @Test
    void stripAnsiMarkupNull() {
        assertEquals("", Ansi.Text.stripAnsiMarkup(null));
    }

    @Test
    void ansiTextMethod() {
        Ansi.Text text = Ansi.AUTO.text("@|bold test|@");
        assertEquals("test", text.toString());
    }
}
