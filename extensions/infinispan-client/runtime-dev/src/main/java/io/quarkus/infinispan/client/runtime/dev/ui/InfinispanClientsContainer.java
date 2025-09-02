package io.quarkus.infinispan.client.runtime.dev.ui;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.inject.Singleton;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ServerConfiguration;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InjectableBean;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Unremovable;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.infinispan.client.runtime.InfinispanClientUtil;

@IfBuildProfile("dev")
@Unremovable
@Singleton
public class InfinispanClientsContainer {

    /**
     * Used in Dev UI
     *
     * @return info about Infinispan clients
     */
    public List<InfinispanClientInfo> clientsInfo() {
        List<InstanceHandle<RemoteCacheManager>> instanceHandles = Arc.container().listAll(RemoteCacheManager.class);
        List<InfinispanClientInfo> infinispanClientInfos = new ArrayList<>();
        for (InstanceHandle<RemoteCacheManager> ih : instanceHandles) {
            InjectableBean<RemoteCacheManager> bean = ih.getBean();
            Set<Annotation> annotationSet = bean.getQualifiers();
            String identifier = InfinispanClientUtil.DEFAULT_INFINISPAN_CLIENT_NAME;
            for (Annotation annotation : annotationSet) {
                if (annotation instanceof io.quarkus.infinispan.client.InfinispanClientName) {
                    // client name found is found
                    identifier = ((io.quarkus.infinispan.client.InfinispanClientName) annotation).value();
                }
            }
            List<ServerConfiguration> servers = ih.get().getConfiguration().servers();
            if (!servers.isEmpty()) {
                ServerConfiguration firstServer = servers.get(0);
                infinispanClientInfos.add(
                        new InfinispanClientInfo(identifier, firstServer.host() + ":" + firstServer.port()));
            }
        }
        return infinispanClientInfos;
    }

    public static class InfinispanClientInfo {
        public String name;
        public String serverUrl;

        public InfinispanClientInfo(String clientName, String serverUrl) {
            this.name = clientName;
            this.serverUrl = serverUrl;
        }
    }
}
