package io.quarkus.infinispan.client.runtime.jfr.event;

import jdk.jfr.*;

@Label("Cache-Wide Operation Started")
@Category({ "Quarkus", "Cache" })
@Name("quarkus.InfinispanCacheWideStart")
@Description("A cache-wide operation started")
@Enabled(false)
public class CacheWideStartEvent extends AbstractCacheEvent {
}
