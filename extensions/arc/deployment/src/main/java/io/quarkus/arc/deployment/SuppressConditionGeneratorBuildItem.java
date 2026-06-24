package io.quarkus.arc.deployment;

import java.util.function.Consumer;

import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.processor.BeanGenerator;
import io.quarkus.builder.item.MultiBuildItem;

/**
 * This build item can be used to contribute custom logic to the generated method body of
 * {@link InjectableBean#isSuppressed()}.
 * <p>
 * Suppressed beans cannot be obtained by programmatic lookup via {@link jakarta.enterprise.inject.Instance} and
 * {@link io.quarkus.arc.All}.
 * <p>
 * The generator is a {@link Consumer} that receives a {@link BeanGenerator.SuppressConditionGeneration} context providing:
 * <ul>
 * <li>the {@link BeanGenerator.SuppressConditionGeneration#bean() bean} metadata,</li>
 * <li>the {@link BeanGenerator.SuppressConditionGeneration#beanClass() generated class},</li>
 * <li>the {@link BeanGenerator.SuppressConditionGeneration#method() method body} of the {@code isSuppressed()} method.</li>
 * </ul>
 * All generators are executed in sequence to generate the {@code isSuppressed} method body. If a generator decides that the
 * bean should be suppressed, it should call {@link io.quarkus.gizmo2.creator.BlockCreator#returnTrue()}. If no generator
 * returns {@code true}, the bean is not suppressed and the {@code isSuppressed} method returns {@code false}. It is expected
 * that a generator only returns from the
 * {@code isSuppressed} method when it returns {@code true}. In such case,
 * suppress conditions are composed naturally: if one suppress condition is satisfied, the bean is suppressed.
 * A generator may decide to short-circuit this process and return {@code false}, but note that there is no ordering between
 * suppress condition generators at the moment, so this is not recommended. It is perfectly fine not to contribute any bytecode
 * if the bean does not meet certain requirements, e.g. does not implement a specific interface or is not annotated with a
 * specific annotation.
 * <p>
 * The built-in {@link io.quarkus.arc.lookup.LookupIfProperty} and {@link io.quarkus.arc.lookup.LookupUnlessProperty}
 * annotations are implemented using this mechanism.
 *
 * @see BeanGenerator.SuppressConditionGeneration
 * @see InjectableBean#isSuppressed()
 */
public final class SuppressConditionGeneratorBuildItem extends MultiBuildItem {

    private final Consumer<BeanGenerator.SuppressConditionGeneration> generator;

    public SuppressConditionGeneratorBuildItem(Consumer<BeanGenerator.SuppressConditionGeneration> generator) {
        this.generator = generator;
    }

    public Consumer<BeanGenerator.SuppressConditionGeneration> getGenerator() {
        return generator;
    }

}
