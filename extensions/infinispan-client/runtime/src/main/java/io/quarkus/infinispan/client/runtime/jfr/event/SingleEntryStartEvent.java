package io.quarkus.infinispan.client.runtime.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Label("Single-Entry Operation Started")
@Category({ "Quarkus", "Cache" })
@Name("quarkus.InfinispanSingleEntryStart")
@Description("A single-entry cache operation started")
@Enabled(false)
public class SingleEntryStartEvent extends AbstractCacheEvent {
}
