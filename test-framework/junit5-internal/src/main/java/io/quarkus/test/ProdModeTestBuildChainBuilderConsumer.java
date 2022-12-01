package io.quarkus.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildStep;
import io.quarkus.builder.BuildStepBuilder;
import io.quarkus.builder.item.BuildItem;

// needs to be in a class of it's own in order to avoid java.lang.IncompatibleClassChangeError
public class ProdModeTestBuildChainBuilderConsumer implements Consumer<BuildChainBuilder> {
    private final String buildStepClassName;
    private final List<String> producesClassNames;
    private final List<String> consumesClassNames;
    private final Map<String, Object> testContext;

    public ProdModeTestBuildChainBuilderConsumer(String buildStepClassName, List<String> producesClassNames,
            List<String> consumesClassNames, Map<String, Object> testContext) {
        this.buildStepClassName = Objects.requireNonNull(buildStepClassName);
        this.producesClassNames = producesClassNames == null ? Collections.emptyList() : producesClassNames;
        this.consumesClassNames = consumesClassNames == null ? Collections.emptyList() : consumesClassNames;
        this.testContext = testContext;
    }

    @Override
    public void accept(BuildChainBuilder builder) {
        BuildStepBuilder buildStepBuilder;
        ClassLoader cl = this.getClass().getClassLoader();
        try {
            buildStepBuilder = builder.addBuildStep(
                    cl.loadClass(buildStepClassName).asSubclass(BuildStep.class).getConstructor(Map.class)
                            .newInstance(testContext));
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException
                | InvocationTargetException e) {
            throw new IllegalArgumentException("Unable to create build step '" + buildStepClassName + "'", e);
        }
        try {
            for (String producesClassName : producesClassNames) {
                buildStepBuilder.produces(cl.loadClass(producesClassName).asSubclass(BuildItem.class));
            }
            for (String consumesClassName : consumesClassNames) {
                buildStepBuilder.consumes(cl.loadClass(consumesClassName).asSubclass(BuildItem.class));
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to configure build step", e);
        }
        buildStepBuilder.build();
    }
}
