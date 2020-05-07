package io.quarkus.grpc.deployment;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.jandex.ClassType;

import io.quarkus.builder.item.MultiBuildItem;

public final class GrpcServiceBuildItem extends MultiBuildItem {

    ClassType blockingStubClass;
    ClassType mutinyStubClass;
    final String name;

    public GrpcServiceBuildItem(String name) {
        this.name = name;
    }

    public void setBlockingStubClass(ClassType blockingStubClass) {
        if (this.blockingStubClass != null
                && !this.blockingStubClass.name().equals(blockingStubClass.name())) {
            throw new DeploymentException("Invalid gRPC Service - multiple stubs founds for " + name);
        }
        this.blockingStubClass = blockingStubClass;
    }

    public void setMutinyStubClass(ClassType mutinyStubClass) {
        if (this.mutinyStubClass != null
                && !this.mutinyStubClass.name().equals(mutinyStubClass.name())) {
            throw new DeploymentException("Invalid gRPC Service - multiple stubs founds for " + name);
        }

        this.mutinyStubClass = mutinyStubClass;
    }

    public String getServiceName() {
        return name;
    }

    public String getBlockingGrpcServiceName() {
        if (blockingStubClass.name().isInner()) {
            return blockingStubClass.name().prefix().toString();
        } else {
            return blockingStubClass.name().toString();
        }
    }

    public String getMutinyGrpcServiceName() {
        if (mutinyStubClass.name().isInner()) {
            return mutinyStubClass.name().prefix().toString();
        } else {
            return mutinyStubClass.name().toString();
        }
    }
}
