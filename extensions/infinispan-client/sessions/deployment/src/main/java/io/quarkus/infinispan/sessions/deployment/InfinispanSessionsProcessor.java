package io.quarkus.infinispan.sessions.deployment;

import java.util.List;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.infinispan.client.deployment.spi.InfinispanClientBuildItem;
import io.quarkus.infinispan.client.deployment.spi.InfinispanClientNameBuildItem;
import io.quarkus.infinispan.client.runtime.spi.InfinispanConstants;
import io.quarkus.infinispan.sessions.runtime.InfinispanSessionsRecorder;
import io.quarkus.vertx.http.deployment.SessionStoreProviderBuildItem;
import io.quarkus.vertx.http.runtime.HttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.SessionsBuildTimeConfig;

public class InfinispanSessionsProcessor {
    @BuildStep
    public void infinispanClients(HttpBuildTimeConfig httpConfig,
            InfinispanSessionsBuildTimeConfig config,
            BuildProducer<InfinispanClientNameBuildItem> infinispanRequest) {
        if (httpConfig.sessions.mode == SessionsBuildTimeConfig.SessionsMode.INFINISPAN) {
            String clientName = config.clientName.orElse(InfinispanConstants.DEFAULT_INFINISPAN_CLIENT_NAME);
            infinispanRequest.produce(new InfinispanClientNameBuildItem(clientName));
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void infinispanSessions(HttpBuildTimeConfig httpConfig,
            InfinispanSessionsBuildTimeConfig config,
            List<InfinispanClientBuildItem> clients,
            BuildProducer<SessionStoreProviderBuildItem> provider,
            InfinispanSessionsRecorder recorder) {
        if (httpConfig.sessions.mode == SessionsBuildTimeConfig.SessionsMode.INFINISPAN) {
            String clientName = config.clientName.orElse(InfinispanConstants.DEFAULT_INFINISPAN_CLIENT_NAME);
            for (InfinispanClientBuildItem infinispanClient : clients) {
                if (clientName.equals(infinispanClient.getName())) {
                    provider.produce(new SessionStoreProviderBuildItem(recorder.create(infinispanClient.getClient())));
                    return;
                }
            }
            throw new IllegalStateException("Unknown Infinispan client: " + clientName);
        }
    }
}
