package io.quarkus.devtools.codestarts;

import java.util.Map;
import java.util.stream.Stream;

public final class Codestart {
    public static final String BASE_LANGUAGE = "base";
    private final String resourceDir;
    private final CodestartSpec spec;

    public Codestart(final String resourceName, final CodestartSpec spec) {
        this.resourceDir = resourceName;
        this.spec = spec;
    }

    public String getResourceDir() {
        return resourceDir;
    }

    public CodestartSpec getSpec() {
        return spec;
    }

    public Map<String, Object> getLocalData(String languageName) {
        return NestedMaps.deepMerge(Stream.of(getBaseLanguageSpec().getData(), getLanguageSpec(languageName).getData()));
    }

    public Map<String, Object> getSharedData(String languageName) {
        return NestedMaps
                .deepMerge(Stream.of(getBaseLanguageSpec().getSharedData(), getLanguageSpec(languageName).getSharedData()));
    }

    public CodestartSpec.LanguageSpec getBaseLanguageSpec() {
        return getSpec().getLanguagesSpec().getOrDefault(BASE_LANGUAGE, new CodestartSpec.LanguageSpec());
    }

    public CodestartSpec.LanguageSpec getLanguageSpec(String languageName) {
        return getSpec().getLanguagesSpec().getOrDefault(languageName, new CodestartSpec.LanguageSpec());
    }

}
