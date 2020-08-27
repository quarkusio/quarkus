package io.quarkus.kubernetes.deployment;

import java.util.Objects;

import io.dekorate.kubernetes.decorator.NamedResourceDecorator;

public abstract class RemoveLabelSelectorDecorator<T> extends NamedResourceDecorator<T> {

    protected final String label;

    public RemoveLabelSelectorDecorator(String label) {
        this.label = Objects.requireNonNull(label);
    }
}
