package io.quarkus.devtools.codestarts.quarkus;

import static io.quarkus.devtools.codestarts.QuarkusPlatformCodestartResourceLoader.platformPathLoader;
import static io.quarkus.devtools.codestarts.core.CodestartCatalogs.findLanguageName;

import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartCatalogLoader;
import io.quarkus.devtools.codestarts.CodestartException;
import io.quarkus.devtools.codestarts.CodestartPathLoader;
import io.quarkus.devtools.codestarts.CodestartStructureException;
import io.quarkus.devtools.codestarts.CodestartType;
import io.quarkus.devtools.codestarts.DataKey;
import io.quarkus.devtools.codestarts.core.GenericCodestartCatalog;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
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

    public static QuarkusCodestartCatalog fromQuarkusPlatformDescriptor(QuarkusPlatformDescriptor platformDescriptor)
            throws IOException {
        final CodestartPathLoader pathLoader = platformPathLoader(platformDescriptor);
        final Collection<Codestart> codestarts = CodestartCatalogLoader.loadCodestarts(pathLoader, QUARKUS_CODESTARTS_DIR);
        final Map<String, Extension> extensionsMapping = buildExtensionsMapping(platformDescriptor.getExtensions());
        return new QuarkusCodestartCatalog(codestarts, extensionsMapping);
    }

    public static QuarkusCodestartCatalog fromQuarkusPlatformDescriptorAndDirectories(
            QuarkusPlatformDescriptor platformDescriptor, Collection<Path> directories)
            throws IOException {
        final CodestartPathLoader pathLoader = platformPathLoader(platformDescriptor);
        final Map<String, Codestart> codestarts = CodestartCatalogLoader.loadCodestarts(pathLoader, QUARKUS_CODESTARTS_DIR)
                .stream()
                .collect(Collectors.toMap(Codestart::getName, Function.identity()));
        for (Path directory : directories) {
            final Map<String, Codestart> dirCodestarts = CodestartCatalogLoader.loadCodestartsFromDir(directory).stream()
                    .collect(Collectors.toMap(Codestart::getName, Function.identity()));
            // On duplicates, directories override platform codestarts
            codestarts.putAll(dirCodestarts);
        }
        final Map<String, Extension> extensionsMapping = buildExtensionsMapping(platformDescriptor.getExtensions());
        return new QuarkusCodestartCatalog(codestarts.values(), extensionsMapping);
    }

    @Override
    protected Collection<Codestart> select(QuarkusCodestartProjectInput projectInput) {
        // Add codestarts from extension and for tooling
        projectInput.getSelection().addNames(getExtensionCodestarts(projectInput));
        projectInput.getSelection().addNames(getToolingCodestarts(projectInput));
        projectInput.getSelection().addNames(projectInput.getOverrideExamples());

        projectInput.getData().putAll(generateSelectedExtensionsData(projectInput));

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

        projectInput.getData().putAll(generateSelectedExamplesData(projectCodestarts));

        return projectCodestarts;
    }

    private Set<String> getExtensionCodestarts(QuarkusCodestartProjectInput projectInput) {
        return getSelectedExtensionsAsStream(projectInput)
                .map(Extension::getCodestart)
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

    private Map<String, Object> generateSelectedExamplesData(List<Codestart> projectCodestarts) {
        Map<String, Object> data = new HashMap<>();
        data.put("selected-examples", projectCodestarts.stream().filter(QuarkusCodestartCatalog::isExample).map(c -> {
            Map<String, Object> eData = new HashMap<>();
            eData.put("name", c.getName());
            eData.put("tags", c.getTags());
            eData.putAll(c.getMetadata());
            return eData;
        }).collect(Collectors.toList()));
        return data;
    }

    private Map<String, Object> generateSelectedExtensionsData(QuarkusCodestartProjectInput projectInput) {
        Map<String, Object> data = new HashMap<>();
        data.put("selected-extensions-ga",
                getSelectedExtensionsAsStream(projectInput).map(Extension::managementKey).collect(Collectors.toSet()));
        data.put("selected-extensions", getSelectedExtensionsAsStream(projectInput).map(e -> {
            Map<String, Object> eData = new HashMap<>();
            eData.put("name", e.getName());
            eData.put("description", e.getDescription());
            eData.put("guide", e.getGuide());
            return eData;
        }).collect(Collectors.toList()));
        return data;
    }

    public static boolean isExample(Codestart codestart) {
        return codestart.getType() == CodestartType.CODE && codestart.getSpec().getTags().contains(Tag.EXAMPLE.key());
    };

    private static Map<String, Extension> buildExtensionsMapping(Collection<Extension> extensions) {
        return extensions.stream()
                .collect(Collectors.toMap(Extensions::toGA, Function.identity()));
    }
}
