package io.quarkus.qute.deployment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    public void testEmptyContentAndValues() {
        Map<String, TemplateInfo> templates = new LinkedHashMap<>();
        templates.put("empty.html", new TemplateInfo("empty.html", "", ""));
        templates.put("", new TemplateInfo("", null, null));
        Map<String, List<String>> variants = Map.of("empty", List.of(""), "none", List.of());
        Map<ParserConfig, Set<String>> configs = Map.of(new ParserConfig('='), Set.of(""));

        QuteContextData read = QuteContextData.read(new QuteContextData(templates, variants, configs).write());

        assertSameData(new QuteContextData(templates, variants, configs), read);
        assertEquals("", read.templates().get("empty.html").content);
        assertEquals("", read.templates().get("empty.html").source);
    }

    @Test
    public void testRandomRoundTrip() {
        for (long seed = 0; seed < 500; seed++) {
            Random random = new Random(seed);
            QuteContextData data = randomData(random);
            byte[] bytes = data.write();
            QuteContextData read;
            try {
                read = QuteContextData.read(bytes);
            } catch (RuntimeException e) {
                throw new AssertionError("seed " + seed + ": " + new String(bytes, StandardCharsets.UTF_8), e);
            }
            assertSameData(data, read, "seed " + seed);
            assertArrayEquals(bytes, read.write(), "seed " + seed);
        }
    }

    @Test
    public void testInvalidText() {
        List<String> invalid = List.of(
                "",
                "other 1\n",
                "qute-context-data 1",
                "qute-context-data 1\ntemplate a",
                "qute-context-data 1\ntemplate\n",
                "qute-context-data 1\ntemplate a b\n",
                "qute-context-data 1\nsource s\n",
                "qute-context-data 1\ncontent 1\nx\n",
                "qute-context-data 1\ntemplate a\ncontent 10\nshort\n",
                "qute-context-data 1\ntemplate a\ncontent x\n\n",
                "qute-context-data 1\ntemplate a\ncontent -1\n\n",
                "qute-context-data 1\ntemplate a\ncontent 2147483647\n\n",
                "qute-context-data 1\ntemplate a%\n",
                "qute-context-data 1\ntemplate a%zz\n",
                "qute-context-data 1\nvariant\n",
                "qute-context-data 1\nparser-config\n",
                "qute-context-data 1\nparser-config ab x\n",
                "qute-context-data 1\nparser-config %%\n",
                "qute-context-data 1\nparser-config a x\n",
                "qute-context-data 1\nunknown\n");
        for (String text : invalid) {
            IllegalStateException e = assertThrows(IllegalStateException.class,
                    () -> QuteContextData.read(text.getBytes(StandardCharsets.UTF_8)), text);
            assertTrue(e.getMessage().startsWith("Invalid " + QuteContextData.RESOURCE), e.getMessage());
        }
    }

    private static final List<String> PIECES = List.of("", "a", "Z", "/", ".html", " ", "  ", "\t", "\n", "\r", "\u0000",
            "\u007f", "%", "%2", "%25", "%zz", "-", "=", "template", "content", "content 3", "source", "variant",
            "parser-config", "qute-context-data 1", "é", "\uD83D\uDE00", "{name}", "\\");

    private static String randomString(Random random) {
        StringBuilder value = new StringBuilder();
        int count = random.nextInt(5);
        for (int i = 0; i < count; i++) {
            value.append(PIECES.get(random.nextInt(PIECES.size())));
        }
        return value.toString();
    }

    private static QuteContextData randomData(Random random) {
        Map<String, TemplateInfo> templates = new HashMap<>();
        for (int i = random.nextInt(6); i > 0; i--) {
            String path = randomString(random);
            templates.put(path, new TemplateInfo(path, random.nextBoolean() ? null : randomString(random),
                    random.nextBoolean() ? null : randomString(random)));
        }
        Map<String, List<String>> variants = new HashMap<>();
        for (int i = random.nextInt(4); i > 0; i--) {
            List<String> values = new ArrayList<>();
            for (int j = random.nextInt(4); j > 0; j--) {
                values.add(randomString(random));
            }
            variants.put(randomString(random), values);
        }
        Map<ParserConfig, Set<String>> configs = new HashMap<>();
        String commands = "=-% \n\u0000$*~";
        for (int i = random.nextInt(3); i > 0; i--) {
            Set<String> paths = new HashSet<>();
            for (int j = random.nextInt(4); j > 0; j--) {
                paths.add(randomString(random));
            }
            configs.put(new ParserConfig(commands.charAt(random.nextInt(commands.length()))), paths);
        }
        return new QuteContextData(templates, variants, configs);
    }

    private static void assertSameData(QuteContextData expected, QuteContextData actual) {
        assertSameData(expected, actual, "");
    }

    private static void assertSameData(QuteContextData expected, QuteContextData actual, String message) {
        assertEquals(expected.templates().keySet(), actual.templates().keySet(), message);
        for (TemplateInfo template : expected.templates().values()) {
            TemplateInfo read = actual.templates().get(template.path);
            assertEquals(template.path, read.path, message);
            assertEquals(template.source, read.source, message);
            assertEquals(template.content, read.content, message);
        }
        assertEquals(expected.variants(), actual.variants(), message);
        assertEquals(expected.nonDefaultParserConfigs(), actual.nonDefaultParserConfigs(), message);
    }

}
