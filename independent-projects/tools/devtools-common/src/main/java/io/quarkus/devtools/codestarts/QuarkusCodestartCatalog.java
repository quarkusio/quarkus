package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.core.CodestartCatalogs.findLanguageName;

import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.codestarts.core.GenericCodestartCatalog;
import io.quarkus.devtools.project.extensions.Extensions;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class QuarkusCodestartCatalog extends GenericCodestartCatalog<QuarkusCodestartProjectInput> {

    public static final String QUARKUS_CODESTARTS_DIR = "codestarts/quarkus";
    private final Map<String, String> extensionCodestartMapping;

    public enum Tag implements KeySupplier {
        EXAMPLE,
        SINGLETON_EXAMPLE,
        MAVEN_ONLY;
    }

    public enum Language implements KeySupplier {
        JAVA,
        KOTLIN,
        SCALA
    }

    public enum Tooling implements KeySupplier {
        GRADLE_WRAPPER,
        MAVEN_WRAPPER,
        DOCKERFILES
    }

    public enum Example implements KeySupplier {
        RESTEASY_EXAMPLE,
        COMMANDMODE_EXAMPLE
    }

    private QuarkusCodestartCatalog(Collection<Codestart> codestarts,
            Map<String, String> extensionCodestartMapping) {
        super(codestarts);
        this.extensionCodestartMapping = extensionCodestartMapping;
    }

    public static QuarkusCodestartCatalog fromQuarkusPlatformDescriptor(QuarkusPlatformDescriptor platformDescriptor)
            throws IOException {
        final CodestartPathLoader pathLoader = platformPathLoader(platformDescriptor);
        final Collection<Codestart> codestarts = CodestartCatalogLoader.loadCodestarts(pathLoader, QUARKUS_CODESTARTS_DIR);
        final Map<String, String> extensionCodestartMapping = buildCodestartMapping(platformDescriptor.getExtensions());
        return new QuarkusCodestartCatalog(codestarts, extensionCodestartMapping);
    }

    public static QuarkusCodestartCatalog fromQuarkusPlatformDescriptorAndDirectories(
            QuarkusPlatformDescriptor platformDescriptor, Collection<Path> directories)
            throws IOException {
        final CodestartPathLoader pathLoader = platformPathLoader(platformDescriptor);
        final ArrayList<Codestart> codestarts = new ArrayList<>(
                CodestartCatalogLoader.loadCodestarts(pathLoader, QUARKUS_CODESTARTS_DIR));
        for (Path directory : directories) {
            codestarts.addAll(CodestartCatalogLoader.loadUserDirectoryCodestarts(directory));
        }
        final Map<String, String> extensionCodestartMapping = buildCodestartMapping(platformDescriptor.getExtensions());
        return new QuarkusCodestartCatalog(codestarts, extensionCodestartMapping);
    }

    @Override
    public CodestartProjectDefinition createProject(QuarkusCodestartProjectInput projectInput) {
        return super.createProject(projectInput);
    }

    @Override
    protected Collection<Codestart> select(QuarkusCodestartProjectInput projectInput) {
        // Add codestarts from extension and for tooling
        projectInput.getSelection().addNames(getExtensionCodestarts(projectInput));
        projectInput.getSelection().addNames(getToolingCodestarts(projectInput));

        // Filter out examples if noExamples
        final List<Codestart> projectCodestarts = super.select(projectInput).stream()
                .filter(c -> !isExample(c) || !projectInput.noExamples())
                .collect(Collectors.toCollection(ArrayList::new));

        // include default example codestarts if none selected
        if (!projectInput.noExamples()
                && projectCodestarts.stream()
                        .noneMatch(c -> isExample(c) && !c.getSpec().isPreselected())) {
            final Codestart defaultCodestart = codestarts.stream()
                    .filter(c -> c.isSelected(Collections.singleton(Example.RESTEASY_EXAMPLE.getKey())))
                    .findFirst().orElseThrow(() -> new CodestartStructureException(
                            Example.RESTEASY_EXAMPLE.getKey() + " codestart not found"));
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
                .filter(c -> c.containsTag(Tag.SINGLETON_EXAMPLE.getKey()))
                .count();

        if (examplesWithCompatIssues == 1) {
            // remove other examples
            projectCodestarts.removeIf(c -> isExample(c) && !c.containsTag(Tag.SINGLETON_EXAMPLE.getKey()));
        } else if (examplesWithCompatIssues > 1) {
            throw new CodestartException(
                    "Only one extension with singleton example can be selected at a time (you can always use 'noExamples' if needed)");
        }

        return projectCodestarts;
    }

    private Set<String> getExtensionCodestarts(QuarkusCodestartProjectInput projectInput) {
        return projectInput.getExtensions().stream()
                .map(Extensions::toGA)
                .filter(extensionCodestartMapping::containsKey)
                .map(extensionCodestartMapping::get)
                .collect(Collectors.toSet());
    }

    private List<String> getToolingCodestarts(QuarkusCodestartProjectInput projectInput) {
        final List<String> codestarts = new ArrayList<>();
        codestarts.add(projectInput.getBuildTool().getKey());
        if (!projectInput.noBuildToolWrapper()) {
            switch (projectInput.getBuildTool()) {
                case GRADLE:
                case GRADLE_KOTLIN_DSL:
                    codestarts.add(QuarkusCodestartCatalog.Tooling.GRADLE_WRAPPER.getKey());
                    break;
                case MAVEN:
                    codestarts.add(QuarkusCodestartCatalog.Tooling.MAVEN_WRAPPER.getKey());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported build tool wrapper: " + projectInput.getBuildTool());
            }
        }
        if (!projectInput.noDockerfiles()) {
            codestarts.add(QuarkusCodestartCatalog.Tooling.DOCKERFILES.getKey());
        }
        return codestarts;
    }

    public static CodestartPathLoader platformPathLoader(QuarkusPlatformDescriptor platformDescr) {
        return new QuarkusPlatformCodestartResourceLoader(platformDescr);
    }

    public static boolean isExample(Codestart codestart) {
        return codestart.getType() == CodestartType.CODE && codestart.getSpec().getTags().contains(Tag.EXAMPLE.getKey());
    }

    private static class QuarkusPlatformCodestartResourceLoader implements CodestartPathLoader {
        private QuarkusPlatformDescriptor platformDescr;

        QuarkusPlatformCodestartResourceLoader(QuarkusPlatformDescriptor platformDescr) {
            this.platformDescr = platformDescr;
        }

        @Override
        public <T> T loadResourceAsPath(String name, PathConsumer<T> consumer) throws IOException {
            return platformDescr.loadResourceAsPath(name, consumer::consume);
        }
    };

    interface KeySupplier {
        default String getKey() {
            return this.toString().toLowerCase().replace("_", "-");
        }
    }

    private static Map<String, String> buildCodestartMapping(Collection<Extension> extensions) {
        return extensions.stream()
                .filter(e -> e.getCodestart() != null)
                .collect(Collectors.toMap(Extensions::toGA, Extension::getCodestart));
    }

}
