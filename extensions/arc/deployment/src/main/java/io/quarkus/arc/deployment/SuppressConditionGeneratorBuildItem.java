package io.quarkus.arc.deployment;

import java.util.function.Consumer;
import java.util.function.Function;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gizmo.BytecodeCreator;

/**
 * This build item can be used to contribute logic to the generated method body of {@link InjectableBean#isSuppressed()}.
 */
final class SuppressConditionGeneratorBuildItem extends MultiBuildItem {

    private final Function<BeanInfo, Consumer<BytecodeCreator>> generator;

    public SuppressConditionGeneratorBuildItem(Function<BeanInfo, Consumer<BytecodeCreator>> generator) {
        this.generator = generator;
    }

    public Function<BeanInfo, Consumer<BytecodeCreator>> getGenerator() {
        return generator;
    }

}
