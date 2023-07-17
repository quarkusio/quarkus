
package io.quarkus.kubernetes.deployment;

import io.dekorate.SelectorDecoratorFactory;
import io.dekorate.kubernetes.decorator.AddToMatchingLabelsDecorator;
import io.dekorate.kubernetes.decorator.RemoveFromMatchingLabelsDecorator;

public class JobSelectorDecoratorFactory implements SelectorDecoratorFactory {

    @Override
    public AddToMatchingLabelsDecorator createAddToSelectorDecorator(String name, String key, String value) {
        return new AddToMatchingLabelsDecorator(name, key, value);
    }

    @Override
    public RemoveFromMatchingLabelsDecorator createRemoveFromSelectorDecorator(String name, String key) {
        return new RemoveFromMatchingLabelsDecorator(name, key);
    }

    @Override
    public String kind() {
        return "Job";
    }
}
