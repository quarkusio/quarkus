package io.quarkus.rest.client.reactive.jackson.runtime.serialisers;

import org.jboss.resteasy.reactive.client.impl.RestClientClosingTask;

import io.quarkus.rest.client.reactive.jackson.ClientObjectMapper;

/**
 * Cleans up the mappings that is needed to support {@link ClientObjectMapper}
 */
public class JacksonCleanupRestClientClosingTask implements RestClientClosingTask {

    @Override
    public void close(Context context) {
        JacksonUtil.contextResolverMap
                .remove(new ResolverMapKey(context.baseTarget().getConfiguration(), context.restApiClass()));
    }
}
