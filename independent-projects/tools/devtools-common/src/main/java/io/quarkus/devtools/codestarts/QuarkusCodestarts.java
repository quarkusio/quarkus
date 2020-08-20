package io.quarkus.devtools.codestarts;

import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.dependencies.Extension;
import io.quarkus.devtools.project.BuildTool;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class QuarkusCodestarts {
    private QuarkusCodestarts() {
    }

    public enum Language implements CodestartName {
        JAVA,
        KOTLIN,
        SCALA
    }

    public enum Tooling implements CodestartName {
        GRADLE_WRAPPER,
        MAVEN_WRAPPER,
        DOCKERFILES
    }

    public static CodestartInputBuilder inputBuilder(QuarkusPlatformDescriptor platformDescr) {
        return CodestartInput.builder(resourceLoader(platformDescr), buildCodestartMapping(platformDescr.getExtensions()))
                .includeExamples();
    }

    public static CodestartResourceLoader resourceLoader(QuarkusPlatformDescriptor platformDescr) {
        return new QuarkusPlatformCodestartResourceLoader(platformDescr);
    }

    public static Map<AppArtifactKey, String> buildCodestartMapping(Collection<Extension> extensions) {
        return extensions.stream()
                .filter(e -> e.getCodestart() != null)
                .collect(Collectors.toMap(e -> new AppArtifactKey(e.getGroupId(), e.getArtifactId(), e.getClassifier(),
                        e.getType() == null ? "jar" : e.getType()), Extension::getCodestart));
    }

    public static List<String> getToolingCodestarts(final BuildTool buildTool,
            boolean noBuildToolWrapper,
            boolean noDockerfiles) {
        final List<String> codestarts = new ArrayList<>();
        codestarts.add(buildTool.getKey());
        if (!noBuildToolWrapper) {
            switch (buildTool) {
                case GRADLE:
                case GRADLE_KOTLIN_DSL:
                    codestarts.add(Tooling.GRADLE_WRAPPER.getCodestartName());
                    break;
                case MAVEN:
                    codestarts.add(Tooling.MAVEN_WRAPPER.getCodestartName());
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported build tool wrapper: " + buildTool);
            }
        }
        if (!noDockerfiles) {
            codestarts.add(Tooling.DOCKERFILES.getCodestartName());
        }
        return codestarts;
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

    interface CodestartName {
        default String getCodestartName() {
            return this.toString().toLowerCase().replace("_", "-");
        }
    }
}
