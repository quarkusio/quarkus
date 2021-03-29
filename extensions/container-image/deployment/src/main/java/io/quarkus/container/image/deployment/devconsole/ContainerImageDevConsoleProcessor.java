package io.quarkus.container.image.deployment.devconsole;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.quarkus.container.spi.AvailableContainerImageExtensionBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devconsole.spi.DevConsoleRouteBuildItem;
import io.quarkus.devconsole.spi.DevConsoleTemplateInfoBuildItem;

public class ContainerImageDevConsoleProcessor {

    @BuildStep
    DevConsoleRouteBuildItem builder() {
        return new DevConsoleRouteBuildItem("build", "POST",
                new RebuildHandler(Collections.singletonMap("quarkus.container-image.build", "true")));
    }

    @BuildStep
    DevConsoleTemplateInfoBuildItem handleGetBuilders(List<AvailableContainerImageExtensionBuildItem> extensions) {
        return new DevConsoleTemplateInfoBuildItem("builder",
                extensions.stream().map(s -> new BuilderType(s.getName())).collect(Collectors.toList()));
    }

    public static class BuilderType implements Comparable<BuilderType> {
        private final String name;

        public BuilderType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public int compareTo(BuilderType o) {
            return name.compareTo(o.name);
        }
    }
}
