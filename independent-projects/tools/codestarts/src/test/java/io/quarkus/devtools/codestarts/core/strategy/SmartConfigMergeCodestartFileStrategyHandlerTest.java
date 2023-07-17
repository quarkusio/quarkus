package io.quarkus.devtools.codestarts.core.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

class SmartConfigMergeCodestartFileStrategyHandlerTest {
    @Test
    void testFlatten() {

        final HashMap<String, Object> cc = new HashMap<>();
        cc.put("c-c-a", 1);
        cc.put("c-c-b", 2);

        final HashMap<String, Object> c = new HashMap<>();
        c.put("c-a", "1");
        c.put("c-b", "2");
        c.put("c-c", cc);

        final HashMap<String, Object> config = new HashMap<>();
        config.put("a", "1");
        config.put("b", "2");
        config.put("c", c);

        final HashMap<String, String> flat = new HashMap<>();
        SmartConfigMergeCodestartFileStrategyHandler.flatten("", flat, config);

        assertThat(flat).containsEntry("a", "1");
        assertThat(flat).containsEntry("b", "2");
        assertThat(flat).containsEntry("c.c-a", "1");
        assertThat(flat).containsEntry("c.c-b", "2");
        assertThat(flat).containsEntry("c.c-c.c-c-a", "1");
        assertThat(flat).containsEntry("c.c-c.c-c-b", "2");
    }

    @Test
    void testLogLevel() {
        final HashMap<String, Object> level = new HashMap<>();
        level.put("level", "DEBUG");

        final HashMap<String, Object> categoryName = new HashMap<>();
        categoryName.put("org.hibernate", level);

        final HashMap<String, Object> category = new HashMap<>();
        category.put("category", categoryName);

        final HashMap<String, Object> log = new HashMap<>();
        log.put("log", category);

        final HashMap<String, Object> quarkus = new HashMap<>();
        quarkus.put("quarkus", log);

        final HashMap<String, String> flat = new HashMap<>();
        SmartConfigMergeCodestartFileStrategyHandler.flatten("", flat, quarkus);

        assertThat(flat).containsEntry("quarkus.log.category.\"org.hibernate\".level", "DEBUG");
    }
}
