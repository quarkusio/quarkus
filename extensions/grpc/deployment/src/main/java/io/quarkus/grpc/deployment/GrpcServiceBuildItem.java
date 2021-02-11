package io.quarkus.grpc.deployment;

import java.util.HashSet;
import java.util.Set;

import org.jboss.jandex.ClassType;

import io.quarkus.builder.item.MultiBuildItem;

public final class GrpcServiceBuildItem extends MultiBuildItem {

    final String name;
    Set<ClassType> stubClasses = new HashSet<ClassType>();

    public GrpcServiceBuildItem(String name) {
        this.name = name;
    }

    public Set<ClassType> getStubClasses() {
        return stubClasses;
    }

    public void addStubClass(ClassType stubClass) {
        stubClasses.add(stubClass);
    }

    public String getServiceName() {
        return name;
    }
}
