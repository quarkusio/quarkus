package io.quarkus.smallrye.graphql.client.runtime;

import java.util.Map;

import jakarta.enterprise.context.spi.CreationalContext;

import io.quarkus.arc.BeanDestroyer;

public class GraphQLClientTlsCleanupDestroyer implements BeanDestroyer<Object> {

    @Override
    public void destroy(Object instance, CreationalContext<Object> creationalContext, Map<String, Object> params) {
        SmallRyeGraphQLClientRecorder.TlsClientInfo info = SmallRyeGraphQLClientRecorder
                .removeInstanceTlsInfo(instance);
        if (info != null) {
            SmallRyeGraphQLClientRecorder.removeReloadableHttpClient(info.tlsConfigName(), info.httpClient());
        }
        if (instance instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
