package io.quarkus.infinispan.client.runtime.jfr.event;

import jdk.jfr.Description;
import jdk.jfr.Label;

public class AbstractMultiEntryEvent extends AbstractCacheEvent {

    @Label("Element Count")
    @Description("The number of entries that were targeted")
    protected int elementCount;
}
