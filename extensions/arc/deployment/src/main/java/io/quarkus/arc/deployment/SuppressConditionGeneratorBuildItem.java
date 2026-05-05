package io.quarkus.arc.deployment;

import java.util.function.Consumer;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.processor.BeanGenerator;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item can be used to contribute logic to the generated method body of {@link InjectableBean#isSuppressed()}.
 *
 * @see BeanGenerator.SuppressConditionGeneration
 */
final class SuppressConditionGeneratorBuildItem extends MultiBuildItem {

    private final Consumer<BeanGenerator.SuppressConditionGeneration> generator;

    public SuppressConditionGeneratorBuildItem(Consumer<BeanGenerator.SuppressConditionGeneration> generator) {
        this.generator = generator;
    }

    public Consumer<BeanGenerator.SuppressConditionGeneration> getGenerator() {
        return generator;
    }

}
