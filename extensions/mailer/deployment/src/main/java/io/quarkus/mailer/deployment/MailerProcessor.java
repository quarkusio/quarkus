package io.quarkus.mailer.deployment;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import jakarta.enterprise.inject.Default;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.mailer.MailTemplate;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.MailerName;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.quarkus.mailer.runtime.MailTemplateMailerName;
import io.quarkus.mailer.runtime.MailTemplateProducer;
import io.quarkus.mailer.runtime.MailerRecorder;
import io.quarkus.mailer.runtime.MailerSupport;
import io.quarkus.mailer.runtime.Mailers;
import io.quarkus.mailer.runtime.MailersBuildTimeConfig;
import io.quarkus.mailer.runtime.MailersRuntimeConfig;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.deployment.CheckedTemplateAdapterBuildItem;
import io.quarkus.qute.deployment.QuteProcessor;
import io.quarkus.qute.deployment.TemplatePathBuildItem;
import io.vertx.ext.mail.MailClient;

public class MailerProcessor {

    static final DotName MAIL_TEMPLATE = DotName.createSimple(MailTemplate.class.getName());

    static final DotName MAILER_NAME = DotName.createSimple(MailerName.class);

    static final DotName MAIL_TEMPLATE_MAILER_NAME = DotName.createSimple(MailTemplateMailerName.class);

    private static final List<DotName> SUPPORTED_INJECTION_TYPES = List.of(
            DotName.createSimple(Mailer.class),
            DotName.createSimple(ReactiveMailer.class),
            DotName.createSimple(MockMailbox.class),
            DotName.createSimple(MailClient.class),
            DotName.createSimple(io.vertx.mutiny.ext.mail.MailClient.class),
            MAIL_TEMPLATE);

    public static class CacheAttachmentsEnabled implements BooleanSupplier {
        MailersBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.cacheAttachments;
        }
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(Mailers.class)
                .build());
        beans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClasses(MailTemplateProducer.class)
                .build());
        // add the @MailerName class otherwise it won't be registered as a qualifier
        beans.produce(AdditionalBeanBuildItem.builder()
                .addBeanClass(MailerName.class)
                .build());
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    MailersBuildItem generateMailerSupportBean(MailerRecorder recorder,
            CombinedIndexBuildItem index,
            BeanDiscoveryFinishedBuildItem beans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        List<InjectionPointInfo> mailerInjectionPoints = beans.getInjectionPoints().stream()
                .filter(i -> SUPPORTED_INJECTION_TYPES.contains(i.getRequiredType().name()))
                .collect(Collectors.toList());

        boolean hasDefaultMailer = mailerInjectionPoints.stream()
                .anyMatch(i -> i.hasDefaultedQualifier() ||
                // we inject a MailTemplate and it is not named
                        (MAIL_TEMPLATE.equals(i.getType().name()) && i.getRequiredQualifier(MAILER_NAME) == null))
                || !index.getIndex().getAnnotations(CheckedTemplate.class).isEmpty();

        Set<String> namedMailers = mailerInjectionPoints.stream()
                .map(i -> i.getRequiredQualifier(MAILER_NAME))
                .filter(ai -> ai != null)
                .map(ai -> ai.value().asString())
                .collect(Collectors.toSet());

        MailerSupport mailerSupport = new MailerSupport(hasDefaultMailer, namedMailers);

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(MailerSupport.class)
                .supplier(recorder.mailerSupportSupplier(mailerSupport))
                .scope(Singleton.class)
                .unremovable()
                .done());

        return new MailersBuildItem(hasDefaultMailer, namedMailers);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateMailerBeans(MailerRecorder recorder,
            MailersBuildItem mailers,
            MailersRuntimeConfig mailersRuntimeConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        if (mailers.hasDefaultMailer()) {
            generateMailerBeansForName(Mailers.DEFAULT_MAILER_NAME, recorder, mailersRuntimeConfig, syntheticBeans);
        }

        for (String name : mailers.getNamedMailers()) {
            generateMailerBeansForName(name, recorder, mailersRuntimeConfig, syntheticBeans);
        }
    }

    @BuildStep
    AnnotationsTransformerBuildItem annotationsTransformer() {
        return new AnnotationsTransformerBuildItem(new MailTemplateMailerNameTransformer());
    }

    private void generateMailerBeansForName(String name,
            MailerRecorder recorder,
            MailersRuntimeConfig mailersRuntimeConfig,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans) {
        AnnotationInstance qualifier;
        if (Mailers.DEFAULT_MAILER_NAME.equals(name)) {
            qualifier = AnnotationInstance.builder(Default.class).build();
        } else {
            qualifier = AnnotationInstance.builder(MAILER_NAME).add("value", name).build();
        }

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(MailClient.class)
                .scope(Singleton.class)
                .qualifiers(qualifier)
                .unremovable()
                .defaultBean()
                .setRuntimeInit()
                .addInjectionPoint(ClassType.create(DotName.createSimple(Mailers.class)))
                .createWith(recorder.mailClientFunction(name, mailersRuntimeConfig))
                .done());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(io.vertx.mutiny.ext.mail.MailClient.class)
                .scope(Singleton.class)
                .qualifiers(qualifier)
                .unremovable()
                .defaultBean()
                .setRuntimeInit()
                .addInjectionPoint(ClassType.create(DotName.createSimple(Mailers.class)))
                .createWith(recorder.reactiveMailClientFunction(name, mailersRuntimeConfig))
                .done());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(Mailer.class)
                .scope(Singleton.class)
                .qualifiers(qualifier)
                .unremovable()
                .defaultBean()
                .setRuntimeInit()
                .addInjectionPoint(ClassType.create(DotName.createSimple(Mailers.class)))
                .createWith(recorder.mailerFunction(name, mailersRuntimeConfig))
                .done());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(ReactiveMailer.class)
                .scope(Singleton.class)
                .qualifiers(qualifier)
                .unremovable()
                .defaultBean()
                .setRuntimeInit()
                .addInjectionPoint(ClassType.create(DotName.createSimple(Mailers.class)))
                .createWith(recorder.reactiveMailerFunction(name, mailersRuntimeConfig))
                .done());
        syntheticBeans.produce(SyntheticBeanBuildItem.configure(MockMailbox.class)
                .scope(Singleton.class)
                .qualifiers(qualifier)
                .unremovable()
                .defaultBean()
                .setRuntimeInit()
                .addInjectionPoint(ClassType.create(DotName.createSimple(Mailers.class)))
                .createWith(recorder.mockMailboxFunction(name, mailersRuntimeConfig))
                .done());
    }

    @BuildStep
    CheckedTemplateAdapterBuildItem registerCheckedTemplateAdaptor() {
        return new CheckedTemplateAdapterBuildItem(new MailTemplateInstanceAdaptor());
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem activateSslNativeSupport() {
        return new ExtensionSslNativeSupportBuildItem(Feature.MAILER);
    }

    @BuildStep
    NativeImageConfigBuildItem registerAuthClass(CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {

        // We must register the auth provider used by the Vert.x mail clients
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("io.vertx.ext.mail.impl.sasl.AuthCram",
                "io.vertx.ext.mail.impl.sasl.AuthDigest",
                "io.vertx.ext.mail.impl.sasl.AuthLogin",
                "io.vertx.ext.mail.impl.sasl.AuthPlain").methods().fields().build());

        // Register io.vertx.ext.mail.impl.sasl.NTLMEngineImpl to be initialized at runtime, it uses a static random.
        NativeImageConfigBuildItem.Builder builder = NativeImageConfigBuildItem.builder();
        builder.addRuntimeInitializedClass("io.vertx.ext.mail.impl.sasl.NTLMEngineImpl");

        return builder.build();
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.MAILER);
    }

    @BuildStep(onlyIf = CacheAttachmentsEnabled.class)
    SystemPropertyBuildItem cacheAttachmentBuildItem() {
        return new SystemPropertyBuildItem("vertx.mail.attachment.cache.file", "true");
    }

    @BuildStep
    void validateMailTemplates(
            List<TemplatePathBuildItem> templatePaths, ValidationPhaseBuildItem validationPhase,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {

        Set<String> filePaths = new HashSet<>();
        for (TemplatePathBuildItem templatePath : templatePaths) {
            String filePath = templatePath.getPath();
            if (File.separatorChar != '/') {
                filePath = filePath.replace(File.separatorChar, '/');
            }
            if (filePath.endsWith("html") || filePath.endsWith("htm") || filePath.endsWith("txt")) {
                // For emails, we only consider html and txt templates
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
                AnnotationInstance resourcePath = injectionPoint.getRequiredQualifier(QuteProcessor.LOCATION);
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
