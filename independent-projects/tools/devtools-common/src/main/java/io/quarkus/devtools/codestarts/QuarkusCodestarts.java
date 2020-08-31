package io.quarkus.devtools.codestarts;

import static io.quarkus.devtools.codestarts.CodestartLoader.loadAllCodestarts;

import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class QuarkusCodestarts {
    private QuarkusCodestarts() {
    }

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
        final List<Codestart> allCodestarts = loadAllCodestarts(input.getCodestartInput());
        final CodestartProject codestartProject = Codestarts.prepareProject(input.getCodestartInput(), allCodestarts);

        // Filter out examples if noExamples
        final List<Codestart> codestarts = codestartProject.getCodestarts().stream()
                .filter(c -> !isExample(c) || !input.noExamples())
                .collect(Collectors.toCollection(ArrayList::new));

        // include commandmode example codestarts if none selected
        if (!input.noExamples()
                && codestartProject.getExtraCodestarts().stream()
                        .noneMatch(c -> isExample(c) && !c.getSpec().isPreselected())) {
            final Codestart commandModeCodestart = allCodestarts.stream()
                    .filter(c -> c.isSelected(Collections.singleton(Example.COMMANDMODE_EXAMPLE.getKey())))
                    .findFirst().orElseThrow(() -> new CodestartDefinitionException(
                            Example.COMMANDMODE_EXAMPLE.getKey() + " codestart not found"));
            codestarts.add(commandModeCodestart);
        }

        return new CodestartProject(input.getCodestartInput(), codestarts);
    }

    public static CodestartResourceLoader resourceLoader(QuarkusPlatformDescriptor platformDescr) {
        return new QuarkusPlatformCodestartResourceLoader(platformDescr);
    }

    public static boolean isExample(Codestart codestart) {
        return codestart.getType() == CodestartSpec.Type.CODE && codestart.getSpec().getTags().contains(Tag.EXAMPLE.getKey());
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
