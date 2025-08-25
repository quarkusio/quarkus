package io.quarkus.qute;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

public class NodeResolveTraceLoggingTest {

    @Test
    public void testTraceLog() {
        Logger root = Logger.getLogger("");
        TestHandler handler = new TestHandler();
        root.addHandler(handler);
        Level previousLevel = root.getLevel();
        setLevel(root, Level.FINEST);

        Engine engine = Engine.builder().addDefaults().timeout(200).build();

        assertEquals("Hello world!", engine.parse("Hello {name}!", null, "hello").data("name", "world").render());
        List<LogRecord> records = handler.records;
        assertEquals(2, records.size());
        assertEquals("Resolve {name} started: template [hello:1]", records.get(0).getMessage());
        assertEquals("Resolve {name} completed: template [hello:1]", records.get(1).getMessage());
        records.clear();

        try {
            engine.parse("{foo}", null, "foo").data("foo", new CompletableFuture<>()).render();
            fail();
        } catch (TemplateException expected) {
        }
        assertEquals(1, records.size());
        assertEquals("Resolve {foo} started: template [foo:1]", records.get(0).getMessage());
        records.clear();

        assertEquals("Hello world!", engine.parse("Hello {#if true}world{/if}!", null, "helloIf").render());
        assertEquals(2, records.size());
        assertEquals("Resolve {#if} started: template [helloIf:1]", records.get(0).getMessage());
        assertEquals("Resolve {#if} completed: template [helloIf:1]", records.get(1).getMessage());
        records.clear();

        try {
            engine.parse("{#if foo}{/if}", null, "fooIf").data("foo", new CompletableFuture<>()).render();
            fail();
        } catch (TemplateException expected) {
        }
        assertEquals(1, records.size());
        assertEquals("Resolve {#if} started: template [fooIf:1]", records.get(0).getMessage());
        records.clear();

        try {
            engine.parse("{#if true}{foo}{/if}", null, "fooIf").data("foo", new CompletableFuture<>()).render();
            fail();
        } catch (TemplateException expected) {
        }
        assertEquals(2, records.size());
        assertEquals("Resolve {#if} started: template [fooIf:1]", records.get(0).getMessage());
        assertEquals("Resolve {foo} started: template [fooIf:1]", records.get(1).getMessage());
        records.clear();

        setLevel(root, previousLevel);
    }

    static void setLevel(Logger logger, Level level) {
        logger.setLevel(level);
        for (Handler handler : logger.getHandlers()) {
            handler.setLevel(level);
        }
    }

    static class TestHandler extends Handler {

        final List<LogRecord> records = new CopyOnWriteArrayList<>();

        @Override
        public void publish(LogRecord record) {
            if (record.getLoggerName().equals("io.quarkus.qute.nodeResolve")) {
                records.add(record);
            }
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }

    }

}
