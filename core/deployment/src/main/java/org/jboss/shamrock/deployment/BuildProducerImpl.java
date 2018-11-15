package org.jboss.shamrock.deployment;

import org.jboss.builder.BuildContext;
import org.jboss.builder.item.BuildItem;
import org.jboss.shamrock.annotations.BuildProducer;

/**
 * Producer class used by the source generated from the annotation processor
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
        buildContext.produce(type, item);
    }
}
