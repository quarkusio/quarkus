package io.quarkus.devtools.codestarts;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.codestarts.core.GenericCodestartCatalog;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class QuarkusCodestartCatalog extends GenericCodestartCatalog<QuarkusCodestartProjectInput> {

    public static final String QUARKUS_CODESTARTS_DIR = "codestarts/quarkus";
    private final Map<AppArtifactKey, String> extensionCodestartMapping;

    public enum Tag implements KeySupplier {
        EXAMPLE,
        COMPATIBILITY_ISSUES,
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

    private QuarkusCodestartCatalog(CodestartResourceLoader resourceLoader,
            Collection<Codestart> codestarts,
            Map<AppArtifactKey, String> extensionCodestartMapping) {
        super(resourceLoader, codestarts);
        this.extensionCodestartMapping = extensionCodestartMapping;
    }

    public static QuarkusCodestartCatalog fromQuarkusPlatformDescriptor(QuarkusPlatformDescriptor platformDescriptor)
            throws IOException {
        final CodestartResourceLoader resourceLoader = resourceLoader(platformDescriptor);
        final Collection<Codestart> codestarts = CodestartCatalogLoader.loadCodestarts(resourceLoader, QUARKUS_CODESTARTS_DIR);
        final Map<AppArtifactKey, String> extensionCodestartMapping = buildCodestartMapping(platformDescriptor.getExtensions());
        return new QuarkusCodestartCatalog(resourceLoader, codestarts, extensionCodestartMapping);
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
        final List<Codestart> projectCodestarts = super.select(projectInput.getSelection()).stream()
                .filter(c -> !isExample(c) || !projectInput.noExamples())
                .collect(Collectors.toCollection(ArrayList::new));

        // include commandmode example codestarts if none selected
        if (!projectInput.noExamples()
                && projectCodestarts.stream()
                        .noneMatch(c -> isExample(c) && !c.getSpec().isPreselected())) {
            final Codestart commandModeCodestart = codestarts.stream()
                    .filter(c -> c.isSelected(Collections.singleton(Example.COMMANDMODE_EXAMPLE.getKey())))
                    .findFirst().orElseThrow(() -> new CodestartStructureException(
                            Example.COMMANDMODE_EXAMPLE.getKey() + " codestart not found"));
            projectCodestarts.add(commandModeCodestart);
        }
        return projectCodestarts;
    }

    private Set<String> getExtensionCodestarts(QuarkusCodestartProjectInput projectInput) {
        return projectInput.getDependencies().stream()
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

    public static CodestartResourceLoader resourceLoader(QuarkusPlatformDescriptor platformDescr) {
        return new QuarkusPlatformCodestartResourceLoader(platformDescr);
    }

    public static boolean isExample(Codestart codestart) {
        return codestart.getType() == CodestartType.CODE && codestart.getSpec().getTags().contains(Tag.EXAMPLE.getKey());
    }

    private static class QuarkusPlatformCodestartResourceLoader implements CodestartResourceLoader {
        private QuarkusPlatformDescriptor platformDescr;

        QuarkusPlatformCodestartResourceLoader(QuarkusPlatformDescriptor platformDescr) {
            this.platformDescr = platformDescr;
        }

        @Override
        public <T> T loadResourceAsPath(String name, Consumer<T> consumer) throws IOException {
            return platformDescr.loadResourceAsPath(name, consumer::consume);
        }
    };

    interface KeySupplier {
        default String getKey() {
            return this.toString().toLowerCase().replace("_", "-");
        }
    }

    private static Map<AppArtifactKey, String> buildCodestartMapping(Collection<Extension> extensions) {
        return extensions.stream()
                .filter(e -> e.getCodestart() != null)
                .collect(Collectors.toMap(e -> new AppArtifactKey(e.getGroupId(), e.getArtifactId(), e.getClassifier(),
                        e.getType() == null ? "jar" : e.getType()), Extension::getCodestart));
    }
}
