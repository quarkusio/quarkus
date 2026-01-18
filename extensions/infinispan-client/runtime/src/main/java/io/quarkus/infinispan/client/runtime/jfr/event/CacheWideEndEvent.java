package io.quarkus.infinispan.client.runtime.jfr.event;

import jdk.jfr.*;

@Label("Cache-Wide Operation Completed")
@Category({ "Quarkus", "Cache" })
@Name("quarkus.InfinispanCacheWideEnd")
@Description("A cache-wide operation completed")
@Enabled(false)
public class CacheWideEndEvent extends AbstractCacheEvent {
}
