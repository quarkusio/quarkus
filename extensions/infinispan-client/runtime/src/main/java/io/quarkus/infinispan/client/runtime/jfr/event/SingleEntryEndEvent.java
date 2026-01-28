package io.quarkus.infinispan.client.runtime.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Label("Single-Entry Operation Completed")
@Category({ "Quarkus", "Cache" })
@Name("quarkus.InfinispanSingleEntryEnd")
@Description("A single-entry cache operation completed")
@Enabled(false)
public class SingleEntryEndEvent extends AbstractCacheEvent {
}
