package io.quarkus.grpc.deployment;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class GrpcClientBuildItem extends MultiBuildItem {

    private final String name;
    private final Set<StubInfo> stubs;

    public GrpcClientBuildItem(String name) {
        this.name = name;
        this.stubs = new HashSet<>();
    }

    public Set<StubInfo> getStubs() {
        return stubs;
    }

    public void addStub(DotName stubClass, StubType type) {
        stubs.add(new StubInfo(stubClass, type));
    }

    public String getServiceName() {
        return name;
    }

    public static final class StubInfo {

        public final DotName className;
        public final StubType type;

        public StubInfo(DotName className, StubType type) {
            this.className = className;
            this.type = type;
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
            StubInfo other = (StubInfo) obj;
            return Objects.equals(className, other.className);
        }

    }

    public enum StubType {

        BLOCKING("newBlockingStub"),
        MUTINY("newMutinyStub");

        private final String factoryMethodName;

        StubType(String factoryMethodName) {
            this.factoryMethodName = factoryMethodName;
        }

        public String getFactoryMethodName() {
            return factoryMethodName;
        }
    }
}
