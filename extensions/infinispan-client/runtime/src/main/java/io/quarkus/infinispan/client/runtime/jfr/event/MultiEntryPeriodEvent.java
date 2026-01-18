package io.quarkus.infinispan.client.runtime.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Label("Multi-Entry Operation")
@Category({ "Quarkus", "Cache" })
@Name("quarkus.InfinispanMultiEntry")
@Description("A multi-entry cache operation was in progress")
public class MultiEntryPeriodEvent extends AbstractMultiEntryEvent {
}
