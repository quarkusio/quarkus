package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.CodestartResourceLoader.loadCodestartsFromResources;
import static io.quarkus.devtools.codestarts.core.CodestartCatalogs.findLanguageName;
import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.AppContent.CODE;
import static io.quarkus.devtools.project.CodestartResourceLoadersBuilder.getCodestartResourceLoaders;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getCodestartName;
import static io.quarkus.platform.catalog.processor.ExtensionProcessor.getGuide;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartCatalogLoader;
import io.quarkus.devtools.codestarts.CodestartException;
import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.CodestartType;
import io.quarkus.devtools.codestarts.DataKey;
import io.quarkus.devtools.codestarts.core.GenericCodestartCatalog;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.platform.catalog.processor.ExtensionProcessor;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.registry.catalog.Extension;
import io.quarkus.registry.catalog.ExtensionCatalog;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class QuarkusCodestartCatalog extends GenericCodestartCatalog<QuarkusCodestartProjectInput> {

    public static final String QUARKUS_CODESTARTS_DIR = "codestarts/quarkus";
    public static final String INPUT_SELECTED_EXTENSIONS_KEY = "selected-extensions";
    public static final String INPUT_SELECTED_EXTENSIONS_GA_KEY = "selected-extensions-ga";
    public static final String INPUT_PROVIDED_CODE_KEY = "provided-code";
    private final Map<String, Extension> extensionsMapping;

    public enum AppContent implements DataKey {
        BUILD_TOOL_WRAPPER,
        DOCKERFILES,
        CODE,
    }

    public enum Tag implements DataKey {
        EXTENSION_CODESTART,
        EXAMPLE,
        MAVEN_ONLY;
    }

    public enum Language implements DataKey {
        JAVA,
        KOTLIN,
        SCALA
    }

    public enum Tooling implements DataKey {
        GRADLE_WRAPPER,
        MAVEN_WRAPPER,
        DOCKERFILES
    }

    public enum ExtensionCodestart implements DataKey {
        RESTEASY,
        RESTEAST_REACTIVE,
        SPRING_WEB
    }

    private QuarkusCodestartCatalog(Collection<Codestart> codestarts,
            Map<String, Extension> extensionsMapping) {
        super(codestarts);
        this.extensionsMapping = extensionsMapping;
    }

    public static QuarkusCodestartCatalog fromBaseCodestartsResources(Map<String, Extension> extensionsMapping)
            throws IOException {
        final Map<String, Codestart> codestarts = loadCodestartsFromResources(getCodestartResourceLoaders(),
                QUARKUS_CODESTARTS_DIR);
        return new QuarkusCodestartCatalog(codestarts.values(), extensionsMapping);
    }

    public static QuarkusCodestartCatalog fromBaseCodestartsResources()
            throws IOException {
        return fromBaseCodestartsResources(Collections.emptyMap());
    }

    public static QuarkusCodestartCatalog fromExtensionsCatalogAndDirectories(
            ExtensionCatalog catalog, Collection<Path> directories)
            throws IOException {
        final List<ResourceLoader> loaders = getCodestartResourceLoaders(catalog);
        final Map<String, Codestart> codestarts = loadCodestartsFromResources(loaders,
                QUARKUS_CODESTARTS_DIR);
        for (Path directory : directories) {
            final Map<String, Codestart> dirCodestarts = CodestartCatalogLoader.loadCodestartsFromDir(directory).stream()
                    .collect(Collectors.toMap(Codestart::getName, Function.identity()));
            // On duplicates, directories override platform codestarts
            codestarts.putAll(dirCodestarts);
        }
        final Map<String, Extension> extensionsMapping = buildExtensionsMapping(catalog.getExtensions());
        return new QuarkusCodestartCatalog(codestarts.values(), extensionsMapping);
    }

    public static QuarkusCodestartCatalog fromExtensionsCatalog(ExtensionCatalog catalog,
            List<ResourceLoader> codestartResourceLoaders)
            throws IOException {
        final Map<String, Codestart> codestarts = loadCodestartsFromResources(codestartResourceLoaders, QUARKUS_CODESTARTS_DIR);
        final Map<String, Extension> extensionCodestartMapping = buildExtensionsMapping(catalog.getExtensions());
        return new QuarkusCodestartCatalog(codestarts.values(), extensionCodestartMapping);
    }

    @Override
    protected Collection<Codestart> select(QuarkusCodestartProjectInput projectInput) {
        // Add codestarts from extension and for tooling
        projectInput.getSelection().addNames(getExtensionCodestarts(projectInput));
        projectInput.getSelection().addNames(getToolingCodestarts(projectInput));
        if (projectInput.getExample() != null) {
            projectInput.getSelection().addName(projectInput.getExample());
        }

        // Filter out extension codestarts and examples
        final List<Codestart> projectCodestarts = super.select(projectInput).stream()
                .filter(c -> c.getType() != CodestartType.CODE || projectInput.getAppContent().contains(CODE))
                .filter(c -> !isExample(c) || projectInput.getExample() == null || c.matches(projectInput.getExample()))
                .collect(Collectors.toCollection(ArrayList::new));

        // include default codestarts if no code selected
        if (projectInput.getAppContent().contains(CODE)
                && projectCodestarts.stream()
                        .noneMatch(c -> c.getType() == CodestartType.CODE && !c.getSpec().isPreselected())) {
            final Codestart defaultCodestart = codestarts.stream()
                    .filter(c -> c.matches(ExtensionCodestart.RESTEASY.key()))
                    .findFirst().orElseThrow(() -> new CodestartStructureException(
                            ExtensionCodestart.RESTEASY.key() + " codestart not found"));
            final String languageName = findLanguageName(projectCodestarts);
            if (defaultCodestart.implementsLanguage(languageName)) {
                projectCodestarts.add(defaultCodestart);
            } else {
                projectInput.log().warn(
                        defaultCodestart.getName() + " codestart will not be applied (doesn't implement language '"
                                + languageName
                                + "' yet)");
            }
        }

        // check only one example
        final long examples = projectCodestarts.stream()
                .filter(QuarkusCodestartCatalog::isExample)
                .count();

        if (examples == 1) {
            // remove extension codestarts
            projectCodestarts.removeIf(QuarkusCodestartCatalog::isExtensionCodestart);
        } else if (examples > 1) {
            throw new CodestartException(
                    "Only example can be selected at a time (you can always use 'noCode' if needed)");
        }

        projectInput.getData().putAll(generateSelectionData(projectInput, projectCodestarts));

        return projectCodestarts;
    }

    private Set<String> getExtensionCodestarts(QuarkusCodestartProjectInput projectInput) {
        return getSelectedExtensionsAsStream(projectInput)
                .map(ExtensionProcessor::getCodestartName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Stream<Extension> getSelectedExtensionsAsStream(QuarkusCodestartProjectInput projectInput) {
        return projectInput.getExtensions().stream()
                .map(Extensions::toGA)
                .filter(extensionsMapping::containsKey)
                .map(extensionsMapping::get);
    }

    private List<String> getToolingCodestarts(QuarkusCodestartProjectInput projectInput) {
        final List<String> codestarts = new ArrayList<>();
        codestarts.add(projectInput.getBuildTool().getKey());
        if (projectInput.getAppContent().contains(AppContent.BUILD_TOOL_WRAPPER)) {
            switch (projectInput.getBuildTool()) {
                case GRADLE:
                case GRADLE_KOTLIN_DSL:
                    codestarts.add(QuarkusCodestartCatalog.Tooling.GRADLE_WRAPPER.key());
                    break;
                case MAVEN:
                    codestarts.add(QuarkusCodestartCatalog.Tooling.MAVEN_WRAPPER.key());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported build tool wrapper: " + projectInput.getBuildTool());
            }
        }
        if (projectInput.getAppContent().contains(AppContent.DOCKERFILES)) {
            codestarts.add(QuarkusCodestartCatalog.Tooling.DOCKERFILES.key());
        }
        return codestarts;
    }

    private Map<String, Object> generateSelectionData(QuarkusCodestartProjectInput projectInput,
            List<Codestart> projectCodestarts) {
        Map<String, Object> data = new HashMap<>();
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put(INPUT_PROVIDED_CODE_KEY,
                projectCodestarts.stream().filter(c -> c.getType() == CodestartType.CODE).map(c -> {
                    Map<String, Object> eData = new HashMap<>();
                    eData.put("name", c.getName());
                    eData.put("tags", c.getTags());
                    eData.putAll(c.getMetadata());
                    return eData;
                }).collect(Collectors.toList()));
        inputData.put(INPUT_SELECTED_EXTENSIONS_GA_KEY,
                getSelectedExtensionsAsStream(projectInput).map(Extension::managementKey).collect(Collectors.toSet()));
        inputData.put(INPUT_SELECTED_EXTENSIONS_KEY, getSelectedExtensionsAsStream(projectInput).map(e -> {
            Map<String, Object> eData = new HashMap<>();
            eData.put("name", e.getName());
            eData.put("description", e.getDescription());
            eData.put("guide", getGuide(e));
            return eData;
        }).collect(Collectors.toList()));
        data.put("input", inputData);
        return data;
    }

    public static boolean isExtensionCodestart(Codestart codestart) {
        return codestart.getType() == CodestartType.CODE && codestart.containsTag(Tag.EXTENSION_CODESTART.key());
    }

    public static boolean isExample(Codestart codestart) {
        return codestart.getType() == CodestartType.CODE && codestart.containsTag(Tag.EXAMPLE.key());
    }

    public static Map<String, Extension> buildExtensionsMapping(
            Collection<Extension> extensions) {
        final Map<String, Extension> map = new HashMap<>(extensions.size());
        extensions.forEach(e -> {
            if (getCodestartName(e) != null) {
                map.put(e.getArtifact().getGroupId() + ":" + e.getArtifact().getArtifactId(), e);
            }
        });
        return map;
    }

}
