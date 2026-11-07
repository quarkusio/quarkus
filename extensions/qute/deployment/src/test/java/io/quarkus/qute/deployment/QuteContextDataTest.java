package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import io.quarkus.qute.ParserConfig;
import io.quarkus.qute.runtime.QuteContextData;
import io.quarkus.qute.runtime.QuteRecorder.TemplateInfo;

public class QuteContextDataTest {

    @Test
    public void testRoundTrip() {
        String large = "é".repeat(70_000);
        Map<String, TemplateInfo> templates = Map.of(
                "a.html", new TemplateInfo("a.html", "file:///app/templates/a.html", null),
                "b.txt", new TemplateInfo("b.txt", null, large));
        Map<String, List<String>> variants = Map.of("a", List.of("a.html", "a.txt"));
        Map<ParserConfig, Set<String>> configs = Map.of(new ParserConfig('='), Set.of("a.html", "a"));

        QuteContextData read = QuteContextData.read(new QuteContextData(templates, variants, configs).write());

        assertEquals(2, read.templates().size());
        TemplateInfo a = read.templates().get("a.html");
        assertEquals("file:///app/templates/a.html", a.source);
        assertNull(a.content);
        TemplateInfo b = read.templates().get("b.txt");
        assertNull(b.source);
        assertEquals(large, b.content);
        assertEquals(variants, read.variants());
        assertEquals(configs, read.nonDefaultParserConfigs());
    }

    @Test
    public void testSameBytesForSameData() {
        Map<String, TemplateInfo> first = new LinkedHashMap<>();
        Map<String, TemplateInfo> second = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            first.put("t" + i, new TemplateInfo("t" + i, null, null));
            second.put("t" + (99 - i), new TemplateInfo("t" + (99 - i), null, null));
        }
        assertArrayEquals(new QuteContextData(first, Map.of(), Map.of()).write(),
                new QuteContextData(second, Map.of(), Map.of()).write());
    }

    @Test
    public void testEmpty() {
        QuteContextData read = QuteContextData.read(new QuteContextData(Map.of(), Map.of(), Map.of()).write());
        assertEquals(0, read.templates().size());
        assertEquals(0, read.variants().size());
        assertEquals(0, read.nonDefaultParserConfigs().size());
    }

    @Test
    public void testReadableText() {
        Map<String, TemplateInfo> templates = Map.of(
                "items.html", new TemplateInfo("items.html", "file:///app/templates/items.html", null),
                "tags/hello.html", new TemplateInfo("tags/hello.html", null, "Hello {name}\nBye\n"));
        Map<String, List<String>> variants = Map.of("items", List.of("items.html", "items.txt"));
        Map<ParserConfig, Set<String>> configs = Map.of(new ParserConfig('='), Set.of("items.html"));

        String text = new String(new QuteContextData(templates, variants, configs).write(), StandardCharsets.UTF_8);

        assertEquals("""
                qute-context-data 1
                template items.html
                source file:///app/templates/items.html
                template tags/hello.html
                content 17
                Hello {name}
                Bye

                variant items items.html items.txt
                parser-config = items.html
                """, text);
    }

    @Test
    public void testEncodedValues() {
        String path = "my pages/100% done\n.html";
        Map<String, TemplateInfo> templates = Map.of(path, new TemplateInfo(path, "file:///my%20pages/x", "content"));
        Map<ParserConfig, Set<String>> configs = Map.of(new ParserConfig('-'), Set.of(path),
                new ParserConfig('%'), Set.of("a"));

        byte[] bytes = new QuteContextData(templates, Map.of(), configs).write();
        String text = new String(bytes, StandardCharsets.UTF_8);
        QuteContextData read = QuteContextData.read(bytes);

        assertTrue(text.contains("template my%20pages/100%25%20done%0a.html\n"), text);
        assertEquals(path, read.templates().get(path).path);
        assertEquals("file:///my%20pages/x", read.templates().get(path).source);
        assertEquals(configs, read.nonDefaultParserConfigs());
    }

    @Test
    public void testInvalidText() {
        assertThrows(IllegalStateException.class,
                () -> QuteContextData.read("other 1\n".getBytes(StandardCharsets.UTF_8)));
        assertThrows(IllegalStateException.class, () -> QuteContextData.read(
                "qute-context-data 1\ntemplate a\ncontent 10\nshort\n".getBytes(StandardCharsets.UTF_8)));
    }

}
