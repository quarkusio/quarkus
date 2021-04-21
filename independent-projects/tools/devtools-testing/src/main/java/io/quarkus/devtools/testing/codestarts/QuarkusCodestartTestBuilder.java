package io.quarkus.devtools.testing.codestarts;

import io.quarkus.devtools.codestarts.DataKey;
import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.project.BuildTool;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class QuarkusCodestartTestBuilder {
    public Map<String, Object> data = new HashMap<>();
    BuildTool buildTool;
    Set<String> codestarts;
    Set<Language> languages;

    public QuarkusCodestartTestBuilder codestarts(String... codestarts) {
        this.codestarts = new HashSet<>(Arrays.asList(codestarts));
        return this;
    }

    public QuarkusCodestartTestBuilder languages(Language... languages) {
        this.languages = new HashSet<>(Arrays.asList(languages));
        return this;
    }

    public QuarkusCodestartTestBuilder buildTool(BuildTool buildTool) {
        this.buildTool = buildTool;
        return this;
    }

    public QuarkusCodestartTestBuilder putData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    public QuarkusCodestartTestBuilder putData(DataKey key, Object value) {
        this.data.put(key.key(), value);
        return this;
    }

    public QuarkusCodestartTest build() {
        return new QuarkusCodestartTest(this);
    }
}
