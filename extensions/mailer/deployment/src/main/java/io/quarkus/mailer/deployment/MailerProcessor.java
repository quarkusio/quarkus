package io.quarkus.mailer.deployment;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.runtime.BlockingMailerImpl;
import io.quarkus.mailer.runtime.MailClientProducer;
import io.quarkus.mailer.runtime.MailConfig;
import io.quarkus.mailer.runtime.MailConfigRecorder;
import io.quarkus.mailer.runtime.MailTemplateProducer;
import io.quarkus.mailer.runtime.MockMailboxImpl;
import io.quarkus.mailer.runtime.MutinyMailerImpl;
import io.quarkus.qute.deployment.QuteProcessor;
import io.quarkus.qute.deployment.TemplatePathBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.vertx.ext.mail.MailClient;

public class MailerProcessor {

    private static final DotName MAIL_TEMPLATE = DotName.createSimple(MailTemplate.class.getName());

    @BuildStep
    AdditionalBeanBuildItem registerClients() {
        return AdditionalBeanBuildItem.unremovableOf(MailClientProducer.class);
    }

    @BuildStep
    AdditionalBeanBuildItem registerMailers() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClasses(MutinyMailerImpl.class, BlockingMailerImpl.class, MockMailboxImpl.class,
                        MailTemplateProducer.class)
                .build();
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.MAILER);
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
                shutdown);

        recorder.configureTheMailer(beanContainer.getValue(), config, launchMode.getLaunchMode());

        return new MailerBuildItem(client);
    }

    @BuildStep
    void validateMailTemplates(
            List<TemplatePathBuildItem> templatePaths, ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {

        Set<String> filePaths = new HashSet<String>();
        for (TemplatePathBuildItem templatePath : templatePaths) {
            String filePath = templatePath.getPath();
            if (File.separatorChar != '/') {
                filePath = filePath.replace(File.separatorChar, '/');
            }
            if (filePath.endsWith("html") || filePath.endsWith("htm") || filePath.endsWith("txt")) {
                // For e-mails we only consider html and txt templates
                filePaths.add(filePath);
                int idx = filePath.lastIndexOf('.');
                if (idx != -1) {
                    // Also add version without suffix from the path
                    // For example for "items.html" also add "items"
                    filePaths.add(filePath.substring(0, idx));
                }
            }
        }

        for (InjectionPointInfo injectionPoint : validationPhase.getContext().get(BuildExtension.Key.INJECTION_POINTS)) {
            if (injectionPoint.getRequiredType().name().equals(MAIL_TEMPLATE)) {
                AnnotationInstance resourcePath = injectionPoint.getRequiredQualifier(QuteProcessor.RESOURCE_PATH);
                String name;
                if (resourcePath != null) {
                    name = resourcePath.value().asString();
                } else if (injectionPoint.hasDefaultedQualifier()) {
                    name = QuteProcessor.getName(injectionPoint);
                } else {
                    name = null;
                }
                if (name != null) {
                    // For "@Inject MailTemplate items" we try to match "items"
                    // For "@ResourcePath("github/pulls") MailTemplate pulls" we try to match "github/pulls"
                    if (filePaths.stream().noneMatch(path -> path.endsWith(name))) {
                        validationErrors.produce(new ValidationErrorBuildItem(
                                new IllegalStateException("No mail template found for " + injectionPoint.getTargetInfo())));
                    }
                }
            }
        }
    }
}
