package io.quarkus.deployment;

import io.quarkus.builder.BuildContext;
import io.quarkus.builder.item.BuildItem;
import io.quarkus.deployment.annotations.BuildProducer;

/**
 * Producer class used by the source generated from the annotation processor
 * 
 * @param <T>
 */
@SuppressWarnings("unused")
public class BuildProducerImpl<T extends BuildItem> implements BuildProducer<T> {

    private final Class<T> type;
    private final BuildContext buildContext;

    public BuildProducerImpl(Class<T> type, BuildContext buildContext) {
        this.type = type;
        this.buildContext = buildContext;
    }

    @Override
    public void produce(T item) {
        var someVariable = "value";
        buildContext.produce(type, item);
    }
}
