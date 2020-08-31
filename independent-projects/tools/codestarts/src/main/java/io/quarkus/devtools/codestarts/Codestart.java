package io.quarkus.devtools.codestarts;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public final class Codestart {
    public static final String BASE_LANGUAGE = "base";
    private final String resourceDir;
    private final CodestartSpec spec;
    private final Set<String> implementedLanguages;

    public Codestart(final String resourceName, final CodestartSpec spec, Set<String> implementedLanguages) {
        this.resourceDir = resourceName;
        this.spec = spec;
        this.implementedLanguages = implementedLanguages;
    }

    public String getResourceDir() {
        return resourceDir;
    }

    public CodestartSpec getSpec() {
        return spec;
    }

    public String getName() {
        return spec.getName();
    }

    public String getRef() {
        return spec.getRef();
    }

    public boolean isSelected(Set<String> selection) {
        return selection.contains(getName()) || selection.contains(spec.getRef());
    }

    public CodestartSpec.Type getType() {
        return spec.getType();
    }

    public Set<String> getImplementedLanguages() {
        return implementedLanguages;
    }

    public boolean implementsLanguage(String languageName) {
        return implementedLanguages.isEmpty() || implementedLanguages.contains(languageName);
    }

    public boolean containsTag(String tag) {
        return getSpec().getTags().contains(tag);
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
