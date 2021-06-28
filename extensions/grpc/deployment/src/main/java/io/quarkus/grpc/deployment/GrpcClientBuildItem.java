package io.quarkus.grpc.deployment;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

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
        clients.add(client);
    }

    public String getClientName() {
        return clientName;
    }

    public static final class ClientInfo {

        public final DotName className;
        public final ClientType type;
        public final DotName implName;

        public ClientInfo(DotName className, ClientType type) {
            this(className, type, null);
        }

        public ClientInfo(DotName className, ClientType type, DotName implName) {
            this.className = className;
            this.type = type;
            this.implName = implName;
        }

        @Override
        public int hashCode() {
            return Objects.hash(className);
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
            return Objects.equals(className, other.className);
        }

    }

    public enum ClientType {

        BLOCKING_STUB("newBlockingStub"),
        MUTINY_STUB("newMutinyStub"),
        MUTINY_CLIENT(null);

        private final String factoryMethodName;

        ClientType(String factoryMethodName) {
            this.factoryMethodName = factoryMethodName;
        }

        public String getFactoryMethodName() {
            return factoryMethodName;
        }
    }
}
