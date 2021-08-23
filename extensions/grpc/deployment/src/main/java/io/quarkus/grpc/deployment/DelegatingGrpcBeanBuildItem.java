package io.quarkus.grpc.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

public final class DelegatingGrpcBeanBuildItem extends MultiBuildItem {
    public final ClassInfo generatedBean;
    public final ClassInfo userDefinedBean;

    DelegatingGrpcBeanBuildItem(ClassInfo generatedBean, ClassInfo userDefinedBean) {
        this.generatedBean = generatedBean;
        this.userDefinedBean = userDefinedBean;
    }
}
