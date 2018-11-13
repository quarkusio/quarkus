package org.jboss.shamrock.deployment;

import org.jboss.builder.BuildContext;
import org.jboss.builder.item.BuildItem;
import org.jboss.shamrock.annotations.BuildProducer;

/**
 *
 * @param <T>
 */
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
