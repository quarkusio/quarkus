package io.quarkus.devtools.testing.codestarts;

import io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language;
import io.quarkus.devtools.project.BuildTool;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class QuarkusCodestartTestBuilder {
    BuildTool buildTool;
    Set<String> codestarts;
    Set<Language> languages;
    boolean skipGenerateRealDataProject;
    boolean skipGenerateMockedDataProject;

    public QuarkusCodestartTestBuilder codestarts(String... codestarts) {
        this.codestarts = new HashSet<>(Arrays.asList(codestarts));
        ;
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

    public QuarkusCodestartTestBuilder skipGenerateRealDataProject() {
        this.skipGenerateRealDataProject = true;
        return this;
    }

    public QuarkusCodestartTestBuilder skipGenerateMockedDataProject() {
        this.skipGenerateMockedDataProject = true;
        return this;
    }

    public QuarkusCodestartTest build() {
        return new QuarkusCodestartTest(this);
    }
}
