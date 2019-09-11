package io.quarkus.yaml.configuration.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.yaml.configuration.runtime.YamlConfigSource;

public class YamlConfigSourceTest {

    @Test
    public void test() {

        URL source = getClass().getClassLoader().getResource("yaml-config-source-test.yml");
        int ordinal = 123;
        YamlConfigSource configSource = new YamlConfigSource(source, ordinal);
        Map<String, String> configProperties = configSource.getProperties();

        assertEquals(ordinal, configSource.getOrdinal());
        assertEquals("jdbc:postgresql://localhost/quarkus_test", configProperties.get("quarkus.datasource.url"));
        assertEquals("drop-and-create", configProperties.get("quarkus.hibernate-orm.database.generation"));
        assertEquals("5M", configProperties.get("quarkus.hibernate-orm.cache.\"first-cache\".expiration.max-idle"));
        assertEquals("123", configProperties.get("quarkus.hibernate-orm.cache.\"second-cache\".memory.object-count"));
        assertEquals("GET,POST,Hello\\, World!", configProperties.get("quarkus.http.cors.methods"));
    }
}
