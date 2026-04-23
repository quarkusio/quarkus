package io.quarkus.arc.deployment;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.creator.ClassCreator;

/**
 * This build item can be used to contribute logic to the generated method body of {@link InjectableBean#isSuppressed()}.
 * <p>
 * The generator function receives a {@link BeanInfo} and a {@link ClassCreator} (to allow adding static fields)
 * and returns a {@link Consumer} that contributes to the {@code isSuppressed()} method body.
 */
final class SuppressConditionGeneratorBuildItem extends MultiBuildItem {

    private final BiFunction<BeanInfo, ClassCreator, Consumer<BlockCreator>> generator;

    public SuppressConditionGeneratorBuildItem(BiFunction<BeanInfo, ClassCreator, Consumer<BlockCreator>> generator) {
        this.generator = generator;
    }

    public BiFunction<BeanInfo, ClassCreator, Consumer<BlockCreator>> getGenerator() {
        return generator;
    }

}
