package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.CodestartResourceLoader.loadCodestartsFromResources;
import static io.quarkus.devtools.codestarts.core.CodestartCatalogs.findLanguageName;
import static io.quarkus.devtools.project.QuarkusProjectHelper.getCodestartResourceLoaders;
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
    public static final String INPUT_SELECTED_EXAMPLES_KEY = "selected-examples";
    private final Map<String, Extension> extensionsMapping;

    public enum Tag implements DataKey {
        EXAMPLE,
        SINGLETON_EXAMPLE,
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

    public enum Example implements DataKey {
        RESTEASY_EXAMPLE,
        COMMANDMODE_EXAMPLE
    }

    private QuarkusCodestartCatalog(Collection<Codestart> codestarts,
            Map<String, Extension> extensionsMapping) {
        super(codestarts);
        this.extensionsMapping = extensionsMapping;
    }

    public static QuarkusCodestartCatalog fromQuarkusPlatformDescriptorAndDirectories(
            ExtensionCatalog catalog, Collection<Path> directories)
            throws IOException {
        final Map<String, Codestart> codestarts = loadCodestartsFromResources(getCodestartResourceLoaders(catalog),
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
        projectInput.getSelection().addNames(projectInput.getOverrideExamples());

        // Filter out examples if noExamples
        final List<Codestart> projectCodestarts = super.select(projectInput).stream()
                .filter(c -> !isExample(c) || !projectInput.noExamples())
                .filter(c -> !isExample(c) || projectInput.getOverrideExamples().isEmpty()
                        || c.isSelected(projectInput.getOverrideExamples()))
                .collect(Collectors.toCollection(ArrayList::new));

        // include default example codestarts if none selected
        if (!projectInput.noExamples()
                && projectCodestarts.stream()
                        .noneMatch(c -> isExample(c) && !c.getSpec().isPreselected())) {
            final Codestart defaultCodestart = codestarts.stream()
                    .filter(c -> c.isSelected(Collections.singleton(Example.RESTEASY_EXAMPLE.key())))
                    .findFirst().orElseThrow(() -> new CodestartStructureException(
                            Example.RESTEASY_EXAMPLE.key() + " codestart not found"));
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

        // check compatibility issues
        final long examplesWithCompatIssues = projectCodestarts.stream()
                .filter(QuarkusCodestartCatalog::isExample)
                .filter(c -> c.containsTag(Tag.SINGLETON_EXAMPLE.key()))
                .count();

        if (examplesWithCompatIssues == 1) {
            // remove other examples
            projectCodestarts.removeIf(c -> isExample(c) && !c.containsTag(Tag.SINGLETON_EXAMPLE.key()));
        } else if (examplesWithCompatIssues > 1) {
            throw new CodestartException(
                    "Only one extension with singleton example can be selected at a time (you can always use 'noExamples' if needed)");
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
        if (!projectInput.noBuildToolWrapper()) {
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
        if (!projectInput.noDockerfiles()) {
            codestarts.add(QuarkusCodestartCatalog.Tooling.DOCKERFILES.key());
        }
        return codestarts;
    }

    private Map<String, Object> generateSelectionData(QuarkusCodestartProjectInput projectInput,
            List<Codestart> projectCodestarts) {
        Map<String, Object> data = new HashMap<>();
        final Map<String, Object> inputData = new HashMap<>();
        inputData.put(INPUT_SELECTED_EXAMPLES_KEY,
                projectCodestarts.stream().filter(QuarkusCodestartCatalog::isExample).map(c -> {
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

    public static boolean isExample(Codestart codestart) {
        return codestart.getType() == CodestartType.CODE && codestart.getSpec().getTags().contains(Tag.EXAMPLE.key());
    }

    private static Map<String, Extension> buildExtensionsMapping(
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
