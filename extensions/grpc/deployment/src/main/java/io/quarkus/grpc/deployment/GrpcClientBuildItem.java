package io.quarkus.grpc.deployment;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.grpc.deployment.GrpcClientBuildItem.ClientInfo;
import io.quarkus.grpc.deployment.GrpcClientBuildItem.ClientType;

public final class GrpcClientBuildItem extends MultiBuildItem {

    private final String clientName;
    private final Set<ClientInfo> clients;

    public GrpcClientBuildItem(String name) {
        this.clientName = name;
        this.clients = new HashSet<>();
    }

    public Set<ClientInfo> getClients() {
        return clients;
    }

    public void addClient(ClientInfo client) {
        addClient(client, false);
    }

    public void addClient(ClientInfo client, boolean addChannel) {
        clients.add(client);
        clients.add(new ClientInfo(GrpcDotNames.CHANNEL, ClientType.CHANNEL, client.interceptors));
    }

    public String getClientName() {
        return clientName;
    }

    public static final class ClientInfo {

        public final DotName className;
        public final ClientType type;
        public final DotName implName;
        public final Set<String> interceptors;

        public ClientInfo(DotName className, ClientType type, Set<String> interceptors) {
            this(className, type, null, interceptors);
        }

        public ClientInfo(DotName className, ClientType type, DotName implName, Set<String> interceptors) {
            this.className = className;
            this.type = type;
            this.implName = implName;
            this.interceptors = interceptors;
        }

        @Override
        public int hashCode() {
            return Objects.hash(className, interceptors);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ClientInfo other = (ClientInfo) obj;
            return Objects.equals(className, other.className) && Objects.equals(interceptors, other.interceptors);
        }

    }

    public enum ClientType {

        CHANNEL(null, false),
        BLOCKING_STUB("newBlockingStub", true),
        MUTINY_STUB("newMutinyStub", false),
        MUTINY_CLIENT(null, false);

        private final String factoryMethodName;
        private boolean blocking;

        ClientType(String factoryMethodName, boolean blocking) {
            this.factoryMethodName = factoryMethodName;
            this.blocking = blocking;
        }

        public String getFactoryMethodName() {
            return factoryMethodName;
        }

        public boolean isBlocking() {
            return blocking;
        }
    }
}
