package io.quarkus.devtools.codestarts;

import io.quarkus.devtools.codestarts.core.CodestartSpec;
import io.quarkus.devtools.codestarts.utils.NestedMaps;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public final class Codestart {
    public static final String BASE_LANGUAGE = "base";
    private final CodestartResourceAllocator readerProvider;
    private final CodestartSpec spec;
    private final Set<String> implementedLanguages;

    public Codestart(final CodestartResourceAllocator readerProvider, final CodestartSpec spec,
            Set<String> implementedLanguages) {
        this.readerProvider = readerProvider;
        this.spec = spec;
        this.implementedLanguages = implementedLanguages;
    }

    public void use(Consumer<CodestartResource> readerConsumer) {
        readerProvider.allocate(readerConsumer);
    }

    public CodestartSpec getSpec() {
        return spec;
    }

    public String getName() {
        return spec.getName();
    }

    public Set<String> getTags() {
        return spec.getTags();
    }

    public Map<String, String> getMetadata() {
        return spec.getMetadata();
    }

    public String getRef() {
        return spec.getRef();
    }

    public int getTypeOrder() {
        return spec.getType().getProcessingOrder();
    }

    public boolean matches(String name) {
        return Objects.equals(spec.getName(), name) || Objects.equals(spec.getRef(), name);
    }

    public boolean isSelected(Set<String> selection) {
        return selection.contains(getName()) || selection.contains(spec.getRef());
    }

    public CodestartType getType() {
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
        return NestedMaps.deepMerge(Stream.of(new HashMap<>(getMetadata()), getBaseLanguageSpec().getData(),
                getLanguageSpec(languageName).getData()));
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

    public interface CodestartResourceAllocator {
        void allocate(Consumer<CodestartResource> readerConsumer);
    }
}
