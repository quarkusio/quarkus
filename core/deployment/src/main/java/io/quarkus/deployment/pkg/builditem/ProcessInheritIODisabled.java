package io.quarkus.deployment.pkg.builditem;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildStepBuilder;
import io.quarkus.builder.item.SimpleBuildItem;

/**
 * A build item, which indicates that the {@link ProcessBuilder#inheritIO()} will not work for processes
 * launched by build steps and instead the build step will have to explicitly stream the newly launched
 * process' STDOUT/STDERR, if the data generated on the STDOUT/STDERR of the launched process needs to be
 * made available
 *
 * @see io.quarkus.deployment.util.ProcessUtil
 */
public final class ProcessInheritIODisabled extends SimpleBuildItem {

    /**
     * Generates a {@link List<Consumer<BuildChainBuilder>> build chain builder} which creates a build step
     * producing the {@link ProcessInheritIODisabled} build item
     */
    public static final class Factory implements Function<Map<String, Object>, List<Consumer<BuildChainBuilder>>> {

        @Override
        public List<Consumer<BuildChainBuilder>> apply(final Map<String, Object> props) {
            return Collections.singletonList((builder) -> {
                final BuildStepBuilder stepBuilder = builder.addBuildStep((ctx) -> {
                    ctx.produce(new ProcessInheritIODisabled());
                });
                stepBuilder.produces(ProcessInheritIODisabled.class).build();
            });
        }
    }
}
