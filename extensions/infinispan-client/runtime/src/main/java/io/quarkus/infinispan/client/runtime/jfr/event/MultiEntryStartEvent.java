package io.quarkus.infinispan.client.runtime.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Enabled;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Label("Multi-Entry Operation Started")
@Category({ "Quarkus", "Cache" })
@Name("quarkus.InfinispanMultiEntryStart")
@Description("A multi-entry cache operation started")
@Enabled(false)
public class MultiEntryStartEvent extends AbstractMultiEntryEvent {
}
