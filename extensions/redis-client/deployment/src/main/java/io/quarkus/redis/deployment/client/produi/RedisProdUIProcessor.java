package io.quarkus.redis.deployment.client.produi;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.devjsonrpc.spi.JsonRPCProvidersBuildItem;
import io.quarkus.devui.spi.page.Page;
import io.quarkus.produi.spi.page.ProdUIPageBuildItem;
import io.quarkus.redis.deployment.client.RequestedRedisClientBuildItem;
import io.quarkus.redis.runtime.client.produi.RedisProdUIService;

/**
 * Contributes a read-only Prod UI page listing the configured Redis clients with
 * their connection/pool configuration and a live PING result. There is no Dev UI
 * data page to reuse (the Redis Dev UI is only a Dev Services console link), so a
 * bespoke read-only component + service is provided. The service issues only the
 * {@code PING} command and never exposes credentials. The page is only added when
 * at least one Redis client has been requested.
 */
public class RedisProdUIProcessor {

    // Produced from a zero-input build step: the provider bean registration in the
    // produi extension must not (transitively) depend on Arc/deployment items, or a
    // build-step cycle results. The page step below keeps the client gating.
    @BuildStep
    JsonRPCProvidersBuildItem createProdUIJsonRPCService() {
        return new JsonRPCProvidersBuildItem(RedisProdUIService.class);
    }

    @BuildStep
    void createProdUIPage(List<RequestedRedisClientBuildItem> requestedClients,
            BuildProducer<ProdUIPageBuildItem> prodUIProducer) {
        if (requestedClients.isEmpty()) {
            return;
        }

        ProdUIPageBuildItem page = new ProdUIPageBuildItem();
        page.addPage(Page.webComponentPageBuilder()
                .title("Redis")
                .icon("font-awesome-solid:database")
                .componentLink("pwc-redis-clients.js"));
        prodUIProducer.produce(page);
    }
}
