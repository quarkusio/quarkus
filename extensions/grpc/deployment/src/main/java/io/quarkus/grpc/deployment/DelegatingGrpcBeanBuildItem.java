package io.quarkus.grpc.deployment;

import org.jboss.jandex.ClassInfo;

import io.quarkus.builder.item.MultiBuildItem;

final class DelegatingGrpcBeanBuildItem extends MultiBuildItem {
    final ClassInfo generatedBean;
    final ClassInfo userDefinedBean;

    DelegatingGrpcBeanBuildItem(ClassInfo generatedBean, ClassInfo userDefinedBean) {
        this.generatedBean = generatedBean;
        this.userDefinedBean = userDefinedBean;
    }
}
