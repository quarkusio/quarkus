package io.quarkus.vertx.kotlin.deployment;

import java.util.function.BiConsumer;

import org.jboss.jandex.MethodInfo;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.processor.InvokerBuilder;
import io.quarkus.arc.processor.KotlinUtils;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.vertx.deployment.spi.EventConsumerInvokerCustomizerBuildItem;
import io.quarkus.vertx.kotlin.runtime.ApplicationCoroutineScope;
import io.quarkus.vertx.kotlin.runtime.CoroutineInvoker;

public class VertxKotlinProcessor {
    private static final String KOTLIN_COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope";

    @BuildStep
    void produceCoroutineScope(BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime(KOTLIN_COROUTINE_SCOPE)) {
            return;
        }

        additionalBean.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(ApplicationCoroutineScope.class)
                .setUnremovable()
                .build());
    }

    @BuildStep
    void produceInvokerCustomizerForSuspendConsumeEventMethods(
            BuildProducer<EventConsumerInvokerCustomizerBuildItem> customizers) {
        if (!QuarkusClassLoader.isClassPresentAtRuntime(KOTLIN_COROUTINE_SCOPE)) {
            return;
        }

        customizers.produce(new EventConsumerInvokerCustomizerBuildItem(new BiConsumer<MethodInfo, InvokerBuilder>() {
            @Override
            public void accept(MethodInfo method, InvokerBuilder invokerBuilder) {
                if (KotlinUtils.isKotlinSuspendMethod(method)) {
                    invokerBuilder.withInvocationWrapper(CoroutineInvoker.class, "inNewCoroutine");
                }
            }
        }));
    }
}
