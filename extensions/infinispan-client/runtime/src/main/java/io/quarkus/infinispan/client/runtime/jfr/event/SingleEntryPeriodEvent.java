package io.quarkus.infinispan.client.runtime.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Label("Single-Entry Operation")
@Category({ "Quarkus", "Cache" })
@Name("quarkus.InfinispanSingleEntry")
@Description("A single-entry cache operation was in progress")
public class SingleEntryPeriodEvent extends AbstractCacheEvent {
}
