package io.quarkus.devtools.codestarts.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class NestedMapsTest {

    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final Map<String, Object> NESTED_MAP_1 = readTestYamlMap("/nested-map-1.yml");
    private static final Map<String, Object> NESTED_MAP_2 = readTestYamlMap("/nested-map-2.yml");

    @Test
    void testDeepMerge() {
        final Map<String, Object> target = NestedMaps.deepMerge(NESTED_MAP_1, NESTED_MAP_2);

        checkTargetMap(target);
        ((Map) target.get("hello")).put("world", "changed");
        final Map<String, Object> targetAgain = NestedMaps.deepMerge(NESTED_MAP_1, NESTED_MAP_2);
        checkTargetMap(targetAgain);
    }

    @Test
    void testDeepMergeStream() {
        final Map<String, Object> target = NestedMaps.deepMerge(Stream.of(NESTED_MAP_1, NESTED_MAP_2));

        checkTargetMap(target);
    }

    @Test
    void testGetValue() {
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "foo.baz")).hasValue("baz");
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "foo.int")).hasValue(1);
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "foo.bool")).hasValue(false);
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "foo.foo")).hasValue("bar");
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "foo.bar.baz")).isEmpty();
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "baa")).isEmpty();
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "foo.bar.foo")).hasValue("bar");
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "foo.bar.bar")).hasValue("foo");
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "bar.foo.bar.foo")).hasValue("baz");
        assertThat((Collection) NestedMaps.getValue(NESTED_MAP_1, "list").get()).containsExactly("foo", "bar");
        assertThat(NestedMaps.getValue(NESTED_MAP_1, "bar.foo.bar")).hasValueSatisfying(v -> {
            assertThat(v).isInstanceOf(Map.class);
            assertThat((Map) v).hasFieldOrPropertyWithValue("foo", "baz");
        });
    }

    private void checkTargetMap(Map<String, Object> target) {
        assertThat(NestedMaps.getValue(target, "foo.baz")).hasValue("baz");
        assertThat(NestedMaps.getValue(target, "foo.foo")).hasValue("bar");
        assertThat(NestedMaps.getValue(target, "foo.bar")).hasValue("foo");
        assertThat(NestedMaps.getValue(target, "foo.int")).hasValue(1);
        assertThat(NestedMaps.getValue(target, "foo.bool")).hasValue(false);
        assertThat(NestedMaps.getValue(target, "foo.bar.baz")).isEmpty();
        assertThat(NestedMaps.getValue(target, "baz")).hasValue("bar");

        assertThat(NestedMaps.getValue(target, "bar.foo.bar.foo")).hasValue("bar");
        assertThat(NestedMaps.getValue(target, "bar.foo.bar.baz")).hasValue("foo");
        assertThat(NestedMaps.getValue(target, "hello.world")).hasValue("helloworld");
        assertThat((Collection) NestedMaps.getValue(target, "list").get()).containsExactly("foo", "bar", "baz");
    }

    @Test
    void testUnflatten() {
        final HashMap<String, Object> data = new HashMap<>();
        data.put("foo.baz", "baz");
        data.put("foo.foo", "bar");
        data.put("foo.bar", "foo");
        data.put("foo.int", 1);
        data.put("foo.bool", false);
        data.put("baz", "bar");
        data.put("bar.foo.bar.foo", "bar");
        data.put("bar.foo.bar.baz", "foo");
        data.put("hello", Collections.singletonMap("world", "helloworld"));
        data.put("list", Arrays.asList("foo", "bar", "baz"));
        final Map<String, Object> target = NestedMaps.unflatten(data);

        checkTargetMap(target);
    }

    @Test
    void testUnflattenConflict() {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("foo.baz", "baz");
        data.put("foo", "bar");

        assertThatIllegalStateException()
                .isThrownBy(() -> NestedMaps.unflatten(data))
                .withMessage("Conflicting data types for key 'foo'");
    }

    @Test
    void testUnflattenConflict2() {
        final Map<String, Object> data = new LinkedHashMap<>();
        data.put("foo", "bar");
        data.put("foo.baz", "baz");

        assertThatIllegalStateException()
                .isThrownBy(() -> NestedMaps.unflatten(data))
                .withMessage("Conflicting data types for key 'foo.baz'");
    }

    private static Map<String, Object> readTestYamlMap(String name) {
        try {
            return YAML_MAPPER.readerFor(Map.class).readValue(NestedMapsTest.class.getResourceAsStream(name));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
