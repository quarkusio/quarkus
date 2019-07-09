package io.quarkus.mailer.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.mailer.impl.*;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.ext.mail.MailClient;

public class MailerProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerClients() {
        return AdditionalBeanBuildItem.unremovableOf(MailClientProducer.class);
    }

    @BuildStep
    AdditionalBeanBuildItem registerMailers() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(ReactiveMailerImpl.class, BlockingMailerImpl.class, MockMailboxImpl.class)
                .build();
    }

    @BuildStep
    void registerAuthClass(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        // We must register the auth provider used by the Vert.x mail clients
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, true,
                "io.vertx.ext.mail.impl.sasl.AuthDigestMD5",
                "io.vertx.ext.mail.impl.sasl.AuthCramSHA256",
                "io.vertx.ext.mail.impl.sasl.AuthCramSHA1",
                "io.vertx.ext.mail.impl.sasl.AuthCramMD5",
                "io.vertx.ext.mail.impl.sasl.AuthDigestMD5",
                "io.vertx.ext.mail.impl.sasl.AuthPlain",
                "io.vertx.ext.mail.impl.sasl.AuthLogin"));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    MailerBuildItem build(BuildProducer<FeatureBuildItem> feature, MailConfigRecorder recorder, VertxBuildItem vertx,
            BeanContainerBuildItem beanContainer, LaunchModeBuildItem launchMode, ShutdownContextBuildItem shutdown,
            MailConfig config) {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.MAILER));

        RuntimeValue<MailClient> client = recorder.configureTheClient(vertx.getVertx(), beanContainer.getValue(), config,
                launchMode.getLaunchMode(), shutdown);

        recorder.configureTheMailer(beanContainer.getValue(), config, launchMode.getLaunchMode());

        return new MailerBuildItem(client);
    }
}
