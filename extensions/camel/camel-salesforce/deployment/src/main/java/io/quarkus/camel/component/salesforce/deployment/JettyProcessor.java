package io.quarkus.camel.component.salesforce.deployment;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.ProtocolHandlers;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;

class JettyProcessor {
    private static final List<Class<?>> JETTY_REFLECTIVE_CLASSES = Arrays.asList(
            HttpClient.class,
            ProtocolHandlers.class);

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;

    @BuildStep
    SubstrateConfigBuildItem process() {
        for (Class<?> i : JETTY_REFLECTIVE_CLASSES) {
            addReflectiveClass(false, false, i.getName());
        }
        return SubstrateConfigBuildItem.builder()
                .addRuntimeInitializedClass("org.eclipse.jetty.io.ByteBufferPool")
                .addRuntimeInitializedClass("org.eclipse.jetty.util.thread.QueuedThreadPool")
                .build();
    }

    private void addReflectiveClass(boolean methods, boolean fields, String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, fields, className));
    }
}
