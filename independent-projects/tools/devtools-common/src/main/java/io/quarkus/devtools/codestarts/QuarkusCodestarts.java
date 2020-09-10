package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.CodestartLoader.loadCodestartsFromDefaultDir;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class QuarkusCodestarts {

    private QuarkusCodestarts() {
    }

    public static final String QUARKUS_PROJECT_TYPE = "quarkus";

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

    public static CodestartProject prepareProject(QuarkusCodestartInput input) throws IOException {
        final Collection<Codestart> codestarts = loadQuarkusCodestarts(input.getCodestartInput().getResourceLoader());
        final CodestartProject codestartProject = Codestarts.prepareProject(input.getCodestartInput(), codestarts);

        // Filter out examples if noExamples
        final List<Codestart> projectCodestarts = codestartProject.getCodestarts().stream()
                .filter(c -> !isExample(c) || !input.noExamples())
                .collect(Collectors.toCollection(ArrayList::new));

        // include commandmode example codestarts if none selected
        if (!input.noExamples()
                && projectCodestarts.stream()
                        .noneMatch(c -> isExample(c) && !c.getSpec().isPreselected())) {
            final Codestart commandModeCodestart = codestarts.stream()
                    .filter(c -> c.isSelected(Collections.singleton(Example.COMMANDMODE_EXAMPLE.getKey())))
                    .findFirst().orElseThrow(() -> new CodestartDefinitionException(
                            Example.COMMANDMODE_EXAMPLE.getKey() + " codestart not found"));
            projectCodestarts.add(commandModeCodestart);
        }

        return CodestartProject.of(input.getCodestartInput(), projectCodestarts);
    }

    public static Collection<Codestart> loadQuarkusCodestarts(CodestartResourceLoader loader) throws IOException {
        return loadCodestartsFromDefaultDir(loader, QUARKUS_PROJECT_TYPE);
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
}
