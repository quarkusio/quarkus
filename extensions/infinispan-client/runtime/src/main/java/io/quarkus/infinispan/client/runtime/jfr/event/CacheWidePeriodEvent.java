package io.quarkus.infinispan.client.runtime.jfr.event;

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Label;
import jdk.jfr.Name;

@Label("Cache-Wide Operation")
@Category({ "Quarkus", "Cache" })
@Name("quarkus.InfinispanCacheWide")
@Description("A cache-wide operation was in progress")
public class CacheWidePeriodEvent extends AbstractCacheEvent {
}
