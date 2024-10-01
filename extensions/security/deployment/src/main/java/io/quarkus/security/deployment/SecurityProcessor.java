package io.quarkus.security.deployment;

import static io.quarkus.arc.processor.DotNames.NO_CLASS_INTERCEPTORS;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.security.deployment.DotNames.AUTHENTICATED;
import static io.quarkus.security.deployment.DotNames.DENY_ALL;
import static io.quarkus.security.deployment.DotNames.INHERITED;
import static io.quarkus.security.deployment.DotNames.PERMISSIONS_ALLOWED;
import static io.quarkus.security.deployment.DotNames.PERMIT_ALL;
import static io.quarkus.security.deployment.DotNames.ROLES_ALLOWED;
import static io.quarkus.security.deployment.PermissionSecurityChecks.PermissionSecurityChecksBuilder.getPermissionsAllowedInstances;
import static io.quarkus.security.deployment.PermissionSecurityChecks.PermissionSecurityChecksBuilder.movePermFromMetaAnnToMetaTarget;
import static io.quarkus.security.runtime.SecurityProviderUtils.findProviderIndex;
import static io.quarkus.security.spi.SecurityTransformerUtils.findFirstStandardSecurityAnnotation;
import static io.quarkus.security.spi.SecurityTransformerUtils.hasSecurityAnnotation;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.SynthesisFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.ObserverInfo;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigBuilderBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.gizmo.CatchBlockCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.deployment.PermissionSecurityChecks.PermissionSecurityChecksBuilder;
import io.quarkus.security.runtime.IdentityProviderManagerCreator;
import io.quarkus.security.runtime.QuarkusSecurityRolesAllowedConfigBuilder;
import io.quarkus.security.runtime.SecurityBuildTimeConfig;
import io.quarkus.security.runtime.SecurityCheckRecorder;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.runtime.SecurityIdentityProxy;
import io.quarkus.security.runtime.SecurityProviderRecorder;
import io.quarkus.security.runtime.SecurityProviderUtils;
import io.quarkus.security.runtime.X509IdentityProvider;
import io.quarkus.security.runtime.interceptor.AuthenticatedInterceptor;
import io.quarkus.security.runtime.interceptor.DenyAllInterceptor;
import io.quarkus.security.runtime.interceptor.PermissionsAllowedInterceptor;
import io.quarkus.security.runtime.interceptor.PermitAllInterceptor;
import io.quarkus.security.runtime.interceptor.RolesAllowedInterceptor;
import io.quarkus.security.runtime.interceptor.SecurityCheckStorageBuilder;
import io.quarkus.security.runtime.interceptor.SecurityConstrainer;
import io.quarkus.security.runtime.interceptor.SecurityHandler;
import io.quarkus.security.spi.AdditionalSecuredClassesBuildItem;
import io.quarkus.security.spi.AdditionalSecuredMethodsBuildItem;
import io.quarkus.security.spi.AdditionalSecurityAnnotationBuildItem;
import io.quarkus.security.spi.AdditionalSecurityConstrainerEventPropsBuildItem;
import io.quarkus.security.spi.ClassSecurityCheckAnnotationBuildItem;
import io.quarkus.security.spi.ClassSecurityCheckStorageBuildItem;
import io.quarkus.security.spi.ClassSecurityCheckStorageBuildItem.ClassStorageBuilder;
import io.quarkus.security.spi.DefaultSecurityCheckBuildItem;
import io.quarkus.security.spi.PermissionsAllowedMetaAnnotationBuildItem;
import io.quarkus.security.spi.RolesAllowedConfigExpResolverBuildItem;
import io.quarkus.security.spi.SecurityTransformerUtils;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.DevModeDisabledAuthorizationController;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;

public class SecurityProcessor {

    private static final Logger log = Logger.getLogger(SecurityProcessor.class);
    private static final DotName STARTUP_EVENT_NAME = DotName.createSimple(StartupEvent.class.getName());

    SecurityConfig security;

    /**
     * Create JCAProviderBuildItems for any configured provider names
     */
    @BuildStep
    void produceJcaSecurityProviders(BuildProducer<JCAProviderBuildItem> jcaProviders,
            BuildProducer<BouncyCastleProviderBuildItem> bouncyCastleProvider,
            BuildProducer<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProvider) {
        Set<String> providers = security.securityProviders().orElse(Set.of());
        for (String providerName : providers) {
            if (SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_NAME.equals(providerName)) {
                bouncyCastleProvider.produce(new BouncyCastleProviderBuildItem());
            } else if (SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_NAME.equals(providerName)) {
                bouncyCastleJsseProvider.produce(new BouncyCastleJsseProviderBuildItem());
            } else if (SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_NAME.equals(providerName)) {
                bouncyCastleProvider.produce(new BouncyCastleProviderBuildItem(true));
            } else if (SecurityProviderUtils.BOUNCYCASTLE_FIPS_JSSE_PROVIDER_NAME.equals(providerName)) {
                bouncyCastleJsseProvider.produce(new BouncyCastleJsseProviderBuildItem(true));
            } else {
                jcaProviders
                        .produce(new JCAProviderBuildItem(providerName, security.securityProviderConfig().get(providerName)));
            }
            log.debugf("Added providerName: %s", providerName);
        }
    }

    /**
     * Register the classes for reflection in the requested named providers
     *
     * @param classes - ReflectiveClassBuildItem producer
     * @param jcaProviders - JCAProviderBuildItem for requested providers
     * @throws URISyntaxException
     * @throws MalformedURLException
     */
    @BuildStep
    void registerJCAProvidersForReflection(BuildProducer<ReflectiveClassBuildItem> classes,
            List<JCAProviderBuildItem> jcaProviders,
            BuildProducer<NativeImageSecurityProviderBuildItem> additionalProviders) throws IOException, URISyntaxException {
        for (JCAProviderBuildItem provider : jcaProviders) {
            List<String> providerClasses = registerProvider(provider.getProviderName(), provider.getProviderConfig(),
                    additionalProviders);
            for (String className : providerClasses) {
                classes.produce(ReflectiveClassBuildItem.builder(className).methods().fields().build());
                log.debugf("Register JCA class: %s", className);
            }
        }
    }

    @BuildStep
    void prepareBouncyCastleProviders(CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflection,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReInitialized,
            List<BouncyCastleProviderBuildItem> bouncyCastleProviders,
            List<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProviders) throws Exception {
        Optional<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProvider = getOne(bouncyCastleJsseProviders);
        if (bouncyCastleJsseProvider.isPresent()) {
            reflection.produce(
                    ReflectiveClassBuildItem.builder(SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME).methods()
                            .fields().build());
            reflection.produce(
                    ReflectiveClassBuildItem.builder("org.bouncycastle.jsse.provider.DefaultSSLContextSpi$LazyManagers")
                            .methods().fields().build());
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem(
                            "org.bouncycastle.jsse.provider.DefaultSSLContextSpi$LazyManagers"));
            prepareBouncyCastleProvider(curateOutcomeBuildItem, reflection, runtimeReInitialized,
                    bouncyCastleJsseProvider.get().isInFipsMode());
        } else {
            Optional<BouncyCastleProviderBuildItem> bouncyCastleProvider = getOne(bouncyCastleProviders);
            if (bouncyCastleProvider.isPresent()) {
                prepareBouncyCastleProvider(curateOutcomeBuildItem, reflection, runtimeReInitialized,
                        bouncyCastleProvider.get().isInFipsMode());
            }
        }
    }

    private static void prepareBouncyCastleProvider(CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflection,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReInitialized, boolean isFipsMode) {
        reflection
                .produce(
                        ReflectiveClassBuildItem
                                .builder(isFipsMode ? SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_CLASS_NAME
                                        : SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME)
                                .methods().fields().build());

        if (curateOutcomeBuildItem.getApplicationModel().getDependencies().stream().anyMatch(
                x -> x.getGroupId().equals("org.bouncycastle") && x.getArtifactId().startsWith("bcprov-"))) {
            reflection.produce(ReflectiveClassBuildItem.builder("org.bouncycastle.jcajce.provider.symmetric.AES",
                    "org.bouncycastle.jcajce.provider.symmetric.AES$CBC",
                    "org.bouncycastle.crypto.paddings.PKCS7Padding",
                    "org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi",
                    "org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi$EC",
                    "org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi$ECDSA",
                    "org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi",
                    "org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi$EC",
                    "org.bouncycastle.jcajce.provider.asymmetric.ec.KeyPairGeneratorSpi$ECDSA",
                    "org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyFactorySpi",
                    "org.bouncycastle.jcajce.provider.asymmetric.rsa.KeyPairGeneratorSpi",
                    "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi",
                    "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA256withRSA").methods().fields()
                    .build());
        }
        runtimeReInitialized
                .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.crypto.CryptoServicesRegistrar"));
        if (!isFipsMode) {
            reflection.produce(ReflectiveClassBuildItem.builder("org.bouncycastle.jcajce.provider.drbg.DRBG$Default")
                    .methods().fields().build());
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.jcajce.provider.drbg.DRBG$Default"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.jcajce.provider.drbg.DRBG$NonceAndIV"));
            // URLSeededEntropySourceProvider.seedStream may contain a reference to a 'FileInputStream' which includes
            // references to FileDescriptors which aren't allowed in the image heap
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem(
                            "org.bouncycastle.jcajce.provider.drbg.DRBG$URLSeededEntropySourceProvider"));
        } else {
            reflection.produce(ReflectiveClassBuildItem.builder("org.bouncycastle.crypto.general.AES")
                    .methods().fields().build());
            runtimeReInitialized.produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.crypto.general.AES"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem(
                            "org.bouncycastle.crypto.asymmetric.NamedECDomainParameters"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.crypto.asymmetric.CustomNamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.asn1.ua.DSTU4145NamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.asn1.sec.SECNamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.asn1.cryptopro.ECGOST3410NamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.asn1.x9.X962NamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.asn1.x9.ECNamedCurveTable"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.asn1.anssi.ANSSINamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves"));
            runtimeReInitialized.produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.jcajce.spec.ECUtil"));
        }

        // Reinitialize class because it embeds a java.lang.ref.Cleaner instance in the image heap
        runtimeReInitialized.produce(new RuntimeReinitializedClassBuildItem("sun.security.pkcs11.P11Util"));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void recordBouncyCastleProviders(SecurityProviderRecorder recorder,
            List<BouncyCastleProviderBuildItem> bouncyCastleProviders,
            List<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProviders) {
        Optional<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProvider = getOne(bouncyCastleJsseProviders);
        if (bouncyCastleJsseProvider.isPresent()) {
            if (bouncyCastleJsseProvider.get().isInFipsMode()) {
                recorder.addBouncyCastleFipsJsseProvider();
            } else {
                recorder.addBouncyCastleJsseProvider();
            }
        } else {
            Optional<BouncyCastleProviderBuildItem> bouncyCastleProvider = getOne(bouncyCastleProviders);
            if (bouncyCastleProvider.isPresent()) {
                recorder.addBouncyCastleProvider(bouncyCastleProvider.get().isInFipsMode());
            }
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    NativeImageFeatureBuildItem bouncyCastleFeature(
            List<BouncyCastleProviderBuildItem> bouncyCastleProviders,
            List<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProviders) {

        Optional<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProvider = getOne(bouncyCastleJsseProviders);
        Optional<BouncyCastleProviderBuildItem> bouncyCastleProvider = getOne(bouncyCastleProviders);

        if (bouncyCastleJsseProvider.isPresent() || bouncyCastleProvider.isPresent()) {
            return new NativeImageFeatureBuildItem("io.quarkus.security.BouncyCastleFeature");
        }
        return null;
    }

    @BuildStep
    void addBouncyCastleProvidersToNativeImage(
            BuildProducer<GeneratedNativeImageClassBuildItem> nativeImageClass,
            BuildProducer<NativeImageSecurityProviderBuildItem> additionalProviders,
            List<BouncyCastleProviderBuildItem> bouncyCastleProviders,
            List<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProviders) {

        Optional<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProvider = getOne(bouncyCastleJsseProviders);
        Optional<BouncyCastleProviderBuildItem> bouncyCastleProvider = getOne(bouncyCastleProviders);

        if (bouncyCastleJsseProvider.isPresent() || bouncyCastleProvider.isPresent()) {

            // Prepare BouncyCastle AutoFeature generation
            ClassCreator file = new ClassCreator(new ClassOutput() {
                @Override
                public void write(String s, byte[] bytes) {
                    nativeImageClass.produce(new GeneratedNativeImageClassBuildItem(s, bytes));
                }
            }, "io.quarkus.security.BouncyCastleFeature", null, Object.class.getName(),
                    org.graalvm.nativeimage.hosted.Feature.class.getName());

            MethodCreator afterRegistration = file.getMethodCreator("afterRegistration", "V",
                    org.graalvm.nativeimage.hosted.Feature.AfterRegistrationAccess.class.getName());

            TryBlock overallCatch = afterRegistration.tryBlock();

            if (bouncyCastleJsseProvider.isPresent()) {
                // BCJSSE or BCJSSEFIPS

                additionalProviders.produce(
                        new NativeImageSecurityProviderBuildItem(SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME));

                if (!bouncyCastleJsseProvider.get().isInFipsMode()) {
                    final int sunJsseIndex = findProviderIndex(SecurityProviderUtils.SUN_JSSE_PROVIDER_NAME);

                    ResultHandle bcProvider = overallCatch
                            .newInstance(
                                    MethodDescriptor.ofConstructor(SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME));
                    ResultHandle bcJsseProvider = overallCatch.newInstance(
                            MethodDescriptor.ofConstructor(SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME));

                    overallCatch.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Security.class, "insertProviderAt", int.class, Provider.class,
                                    int.class),
                            bcProvider, overallCatch.load(sunJsseIndex));
                    overallCatch.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Security.class, "insertProviderAt", int.class, Provider.class,
                                    int.class),
                            bcJsseProvider, overallCatch.load(sunJsseIndex + 1));
                } else {
                    final int sunIndex = findProviderIndex(SecurityProviderUtils.SUN_PROVIDER_NAME);

                    ResultHandle bcFipsProvider = overallCatch
                            .newInstance(MethodDescriptor
                                    .ofConstructor(SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_CLASS_NAME));
                    MethodDescriptor bcJsseProviderConstructor = MethodDescriptor.ofConstructor(
                            SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME, "boolean",
                            Provider.class.getName());
                    ResultHandle bcJsseProvider = overallCatch.newInstance(bcJsseProviderConstructor,
                            overallCatch.load(true), bcFipsProvider);

                    overallCatch.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Security.class, "insertProviderAt", int.class, Provider.class,
                                    int.class),
                            bcFipsProvider, overallCatch.load(sunIndex));
                    overallCatch.invokeStaticMethod(
                            MethodDescriptor.ofMethod(Security.class, "insertProviderAt", int.class, Provider.class,
                                    int.class),
                            bcJsseProvider, overallCatch.load(sunIndex + 1));
                }
            } else {
                // BC or BCFIPS

                final String providerName = bouncyCastleProvider.get().isInFipsMode()
                        ? SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_CLASS_NAME
                        : SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME;

                ResultHandle bcProvider = overallCatch.newInstance(MethodDescriptor.ofConstructor(providerName));
                overallCatch.invokeStaticMethod(
                        MethodDescriptor.ofMethod(Security.class, "addProvider", int.class, Provider.class),
                        bcProvider);
            }

            // Complete BouncyCastle AutoFeature generation
            CatchBlockCreator print = overallCatch.addCatch(Throwable.class);
            print.invokeVirtualMethod(ofMethod(Throwable.class, "printStackTrace", void.class), print.getCaughtException());
            afterRegistration.returnValue(null);
            file.close();
        }

    }

    // Work around https://github.com/quarkusio/quarkus/issues/21374
    @BuildStep
    void addBouncyCastleExportsToNativeImage(BuildProducer<JPMSExportBuildItem> jpmsExports,
            List<BouncyCastleProviderBuildItem> bouncyCastleProviders,
            List<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProviders) {
        boolean isInFipsMode;

        Optional<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProvider = getOne(bouncyCastleJsseProviders);
        if (bouncyCastleJsseProvider.isPresent()) {
            isInFipsMode = bouncyCastleJsseProvider.get().isInFipsMode();
        } else {
            Optional<BouncyCastleProviderBuildItem> bouncyCastleProvider = getOne(bouncyCastleProviders);
            isInFipsMode = bouncyCastleProvider.isPresent() && bouncyCastleProvider.get().isInFipsMode();
        }

        if (isInFipsMode) {
            jpmsExports.produce(new JPMSExportBuildItem("java.base", "sun.security.internal.spec"));
            jpmsExports.produce(new JPMSExportBuildItem("java.base", "sun.security.provider"));
        }
    }

    private static <BI extends MultiBuildItem> Optional<BI> getOne(List<BI> items) {
        if (items.size() > 1) {
            throw new IllegalStateException("Only a single Bouncy Castle registration can be provided.");
        }
        return items.stream().findFirst();
    }

    /**
     * Determine the classes that make up the provider and its services
     *
     * @param providerName - JCA provider name
     * @return class names that make up the provider and its services
     */
    private static List<String> registerProvider(String providerName,
            String providerConfig,
            BuildProducer<NativeImageSecurityProviderBuildItem> additionalProviders) {
        List<String> providerClasses = new ArrayList<>();
        Provider provider = Security.getProvider(providerName);
        if (provider != null) {
            providerClasses.add(provider.getClass().getName());
            for (Provider.Service service : provider.getServices()) {
                providerClasses.add(service.getClassName());
                // Need to pull in the key classes
                String supportedKeyClasses = service.getAttribute("SupportedKeyClasses");
                if (supportedKeyClasses != null) {
                    providerClasses.addAll(Arrays.asList(supportedKeyClasses.split("\\|")));
                }
            }

            if (providerConfig != null) {
                Provider configuredProvider = provider.configure(providerConfig);
                if (configuredProvider != null) {
                    Security.addProvider(configuredProvider);
                    providerClasses.add(configuredProvider.getClass().getName());
                }
            }
        }

        if (SecurityProviderUtils.SUN_PROVIDERS.containsKey(providerName)) {
            additionalProviders.produce(
                    new NativeImageSecurityProviderBuildItem(SecurityProviderUtils.SUN_PROVIDERS.get(providerName)));
        }
        return providerClasses;
    }

    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void recordRuntimeConfigReady(SecurityCheckRecorder recorder, ShutdownContextBuildItem shutdownContextBuildItem,
            LaunchModeBuildItem launchModeBuildItem) {
        recorder.setRuntimeConfigReady();
        if (launchModeBuildItem.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            recorder.unsetRuntimeConfigReady(shutdownContextBuildItem);
        }
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void registerSecurityInterceptors(BuildProducer<InterceptorBindingRegistrarBuildItem> registrars,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer, SecurityCheckRecorder recorder,
            Optional<AdditionalSecurityConstrainerEventPropsBuildItem> additionalSecurityConstrainerEventsItem) {
        registrars.produce(new InterceptorBindingRegistrarBuildItem(new SecurityAnnotationsRegistrar()));
        Class<?>[] interceptors = { AuthenticatedInterceptor.class, DenyAllInterceptor.class, PermitAllInterceptor.class,
                RolesAllowedInterceptor.class, PermissionsAllowedInterceptor.class };
        beans.produce(new AdditionalBeanBuildItem(interceptors));
        beans.produce(new AdditionalBeanBuildItem(SecurityHandler.class));

        var additionalEventsSupplier = additionalSecurityConstrainerEventsItem
                .map(AdditionalSecurityConstrainerEventPropsBuildItem::getAdditionalEventPropsSupplier)
                .orElse(null);
        syntheticBeanProducer.produce(SyntheticBeanBuildItem
                .configure(SecurityConstrainer.class)
                .unremovable()
                .scope(Singleton.class)
                .supplier(recorder.createSecurityConstrainer(additionalEventsSupplier))
                .done());
    }

    /**
     * Transform deprecated {@link AdditionalSecuredClassesBuildItem} to {@link AdditionalSecuredMethodsBuildItem}.
     */
    @BuildStep
    void transformAdditionalSecuredClassesToMethods(List<AdditionalSecuredClassesBuildItem> additionalSecuredClassesBuildItems,
            BuildProducer<AdditionalSecuredMethodsBuildItem> additionalSecuredMethodsBuildItemBuildProducer) {
        for (AdditionalSecuredClassesBuildItem additionalSecuredClassesBuildItem : additionalSecuredClassesBuildItems) {
            final Collection<MethodInfo> securedMethods = new ArrayList<>();
            for (ClassInfo additionalSecuredClass : additionalSecuredClassesBuildItem.additionalSecuredClasses) {
                for (MethodInfo method : additionalSecuredClass.methods()) {
                    if (isPublicNonStaticNonConstructor(method)) {
                        securedMethods.add(method);
                    }
                }
            }
            additionalSecuredMethodsBuildItemBuildProducer.produce(
                    new AdditionalSecuredMethodsBuildItem(securedMethods, additionalSecuredClassesBuildItem.rolesAllowed));
        }
    }

    /*
     * The annotation store is not meant to be generally supported for security annotation.
     * It is only used here in order to be able to register the DenyAllInterceptor for
     * methods that don't have a security annotation
     */
    @BuildStep
    void transformSecurityAnnotations(BuildProducer<AnnotationsTransformerBuildItem> transformers,
            List<AdditionalSecuredMethodsBuildItem> additionalSecuredMethods,
            SecurityBuildTimeConfig config) {
        if (config.denyUnannotated()) {
            transformers.produce(new AnnotationsTransformerBuildItem(new DenyingUnannotatedTransformer()));
        }
        if (!additionalSecuredMethods.isEmpty()) {
            for (AdditionalSecuredMethodsBuildItem securedMethods : additionalSecuredMethods) {
                Collection<MethodDescription> additionalSecured = new HashSet<>();
                for (MethodInfo additionalSecuredMethod : securedMethods.additionalSecuredMethods) {
                    additionalSecured.add(createMethodDescription(additionalSecuredMethod));
                }
                if (securedMethods.rolesAllowed.isPresent()) {
                    transformers.produce(
                            new AnnotationsTransformerBuildItem(new AdditionalRolesAllowedTransformer(additionalSecured,
                                    securedMethods.rolesAllowed.get())));
                } else {
                    transformers.produce(
                            new AnnotationsTransformerBuildItem(
                                    new AdditionalDenyingUnannotatedTransformer(additionalSecured)));
                }
            }
        }
    }

    /*
     * Transform all security annotations to be {@code @Inherited}
     */
    @BuildStep
    void makeSecurityAnnotationsInherited(BuildProducer<AnnotationsTransformerBuildItem> transformer) {
        Set<DotName> securityAnnotationNames = Set.of(PERMIT_ALL, DENY_ALL, AUTHENTICATED, PERMISSIONS_ALLOWED, ROLES_ALLOWED);
        transformer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation.forClasses()
                .whenClass(c -> securityAnnotationNames.contains(c.name()))
                .transform(c -> c.add(AnnotationInstance.builder(INHERITED).build()))));
    }

    @BuildStep
    PermissionsAllowedMetaAnnotationBuildItem transformPermissionsAllowedMetaAnnotations(
            BeanArchiveIndexBuildItem beanArchiveBuildItem,
            BuildProducer<AnnotationsTransformerBuildItem> transformers,
            List<ClassSecurityCheckAnnotationBuildItem> classAnnotationItems) {

        var index = beanArchiveBuildItem.getIndex();
        var item = movePermFromMetaAnnToMetaTarget(index);

        // add @PermissionsAllowed to meta-annotation method target
        item.getTransitiveInstances()
                .stream()
                .filter(i -> i.target().kind() == AnnotationTarget.Kind.METHOD)
                .forEach(i -> {
                    var method = i.target().asMethod();
                    var targetClassName = method.declaringClass().name();
                    transformers.produce(
                            new AnnotationsTransformerBuildItem(
                                    AnnotationTransformation
                                            .forMethods()
                                            .whenMethod(targetClassName, method.name())
                                            .transform(tc -> tc.add(i))));
                });

        // extensions WebSockets Next doesn't want CDI interceptors to prevent repeated checks
        var additionalClassAnnotations = classAnnotationItems.stream()
                .map(ClassSecurityCheckAnnotationBuildItem::getClassAnnotation).collect(Collectors.toSet());
        final Predicate<AnnotationInstance> hasNoAdditionalClassAnnotation;
        if (additionalClassAnnotations.isEmpty()) {
            hasNoAdditionalClassAnnotation = ai -> true;
        } else {
            hasNoAdditionalClassAnnotation = ai -> {
                for (var declaredAnnotation : ai.target().asClass().declaredAnnotations()) {
                    if (additionalClassAnnotations.contains(declaredAnnotation.name())) {
                        return false;
                    }
                }
                return true;
            };
        }

        // add @PermissionsAllowed to meta-annotation class target
        item.getTransitiveInstances()
                .stream()
                .filter(i -> i.target().kind() == AnnotationTarget.Kind.CLASS)
                .filter(hasNoAdditionalClassAnnotation)
                .forEach(i -> {
                    var clazz = i.target().asClass();
                    transformers.produce(
                            new AnnotationsTransformerBuildItem(
                                    AnnotationTransformation
                                            .forClasses()
                                            .whenClass(clazz.name())
                                            .transform(tc -> tc.add(i))));
                });

        return item;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    MethodSecurityChecks gatherSecurityChecks(
            BuildProducer<ConfigExpRolesAllowedSecurityCheckBuildItem> configExpSecurityCheckProducer,
            List<RolesAllowedConfigExpResolverBuildItem> rolesAllowedConfigExpResolverBuildItems,
            BeanArchiveIndexBuildItem beanArchiveBuildItem,
            BuildProducer<ApplicationClassPredicateBuildItem> classPredicate,
            BuildProducer<RunTimeConfigBuilderBuildItem> configBuilderProducer,
            List<AdditionalSecuredMethodsBuildItem> additionalSecuredMethods,
            SecurityCheckRecorder recorder, List<AdditionalSecurityAnnotationBuildItem> additionalSecurityAnnotationItems,
            BuildProducer<ClassSecurityCheckStorageBuildItem> classSecurityCheckStorageProducer,
            List<RegisterClassSecurityCheckBuildItem> registerClassSecurityCheckBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<AdditionalSecurityCheckBuildItem> additionalSecurityChecks, SecurityBuildTimeConfig config,
            PermissionsAllowedMetaAnnotationBuildItem permissionsAllowedMetaAnnotationItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer) {
        var hasAdditionalSecAnn = hasAdditionalSecurityAnnotation(additionalSecurityAnnotationItems.stream()
                .map(AdditionalSecurityAnnotationBuildItem::getSecurityAnnotationName).collect(Collectors.toSet()));
        classPredicate.produce(new ApplicationClassPredicateBuildItem(new SecurityCheckStorageAppPredicate()));

        final Map<MethodDescription, AdditionalSecured> additionalSecured = new HashMap<>();
        for (AdditionalSecuredMethodsBuildItem securedMethods : additionalSecuredMethods) {
            for (MethodInfo m : securedMethods.additionalSecuredMethods) {
                additionalSecured.putIfAbsent(createMethodDescription(m),
                        new AdditionalSecured(m, securedMethods.rolesAllowed));
            }
        }

        IndexView index = beanArchiveBuildItem.getIndex();
        Map<MethodInfo, SecurityCheck> securityChecks = gatherSecurityAnnotations(index, configExpSecurityCheckProducer,
                additionalSecured.values(), config.denyUnannotated(), recorder, configBuilderProducer,
                reflectiveClassBuildItemBuildProducer, rolesAllowedConfigExpResolverBuildItems,
                registerClassSecurityCheckBuildItems, classSecurityCheckStorageProducer, hasAdditionalSecAnn,
                additionalSecurityAnnotationItems, permissionsAllowedMetaAnnotationItem, generatedClassesProducer,
                reflectiveClassesProducer);
        for (AdditionalSecurityCheckBuildItem additionalSecurityCheck : additionalSecurityChecks) {
            securityChecks.put(additionalSecurityCheck.getMethodInfo(),
                    additionalSecurityCheck.getSecurityCheck());
        }

        return new MethodSecurityChecks(securityChecks);
    }

    @Consume(Capabilities.class) // make sure extension combinations are validated before default security check
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void createSecurityCheckStorage(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<ApplicationClassPredicateBuildItem> classPredicate,
            SecurityCheckRecorder recorder, MethodSecurityChecks securityChecksItem,
            List<DefaultSecurityCheckBuildItem> defaultSecurityCheckBuildItem) {
        classPredicate.produce(new ApplicationClassPredicateBuildItem(new SecurityCheckStorageAppPredicate()));

        RuntimeValue<SecurityCheckStorageBuilder> builder = recorder.newBuilder();
        for (Map.Entry<MethodInfo, SecurityCheck> methodEntry : securityChecksItem.securityChecks
                .entrySet()) {
            MethodInfo method = methodEntry.getKey();
            String[] params = new String[method.parametersCount()];
            for (int i = 0; i < method.parametersCount(); ++i) {
                params[i] = method.parameterType(i).name().toString();
            }
            recorder.addMethod(builder, method.declaringClass().name().toString(), method.name(), params,
                    methodEntry.getValue());
        }

        if (!defaultSecurityCheckBuildItem.isEmpty()) {
            if (defaultSecurityCheckBuildItem.size() > 1) {
                int itemCount = defaultSecurityCheckBuildItem.size();
                throw new IllegalStateException("Found %d DefaultSecurityCheckBuildItem items, ".formatted(itemCount)
                        + "please make sure the item is produced exactly once");
            }

            var roles = defaultSecurityCheckBuildItem.get(0).getRolesAllowed();
            if (roles == null) {
                recorder.registerDefaultSecurityCheck(builder, recorder.denyAll());
            } else {
                recorder.registerDefaultSecurityCheck(builder, recorder.rolesAllowed(roles.toArray(new String[0])));
            }
        }
        recorder.create(builder);

        syntheticBeans.produce(
                SyntheticBeanBuildItem.configure(SecurityCheckStorage.class)
                        .scope(ApplicationScoped.class)
                        .unremovable()
                        .creator(creator -> {
                            ResultHandle ret = creator.invokeStaticMethod(MethodDescriptor.ofMethod(SecurityCheckRecorder.class,
                                    "getStorage", SecurityCheckStorage.class));
                            creator.returnValue(ret);
                        }).done());
    }

    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void resolveConfigExpressionRoles(Optional<ConfigExpRolesAllowedSecurityCheckBuildItem> configExpRolesChecks,
            SecurityCheckRecorder recorder) {
        if (configExpRolesChecks.isPresent()) {
            // we created supplier security check for each role set with at least one config expression
            // now we need to resolve config expression so that if there are any failures they happen when app starts
            // rather than first time request is checked (which would be more likely to affect end user)
            recorder.resolveRolesAllowedConfigExpRoles();
        }
    }

    private static Map<MethodInfo, SecurityCheck> gatherSecurityAnnotations(IndexView index,
            BuildProducer<ConfigExpRolesAllowedSecurityCheckBuildItem> configExpSecurityCheckProducer,
            Collection<AdditionalSecured> additionalSecuredMethods, boolean denyUnannotated, SecurityCheckRecorder recorder,
            BuildProducer<RunTimeConfigBuilderBuildItem> configBuilderProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<RolesAllowedConfigExpResolverBuildItem> rolesAllowedConfigExpResolverBuildItems,
            List<RegisterClassSecurityCheckBuildItem> registerClassSecurityCheckBuildItems,
            BuildProducer<ClassSecurityCheckStorageBuildItem> classSecurityCheckStorageProducer,
            Predicate<MethodInfo> hasAdditionalSecurityAnnotations,
            List<AdditionalSecurityAnnotationBuildItem> additionalSecurityAnnotationItems,
            PermissionsAllowedMetaAnnotationBuildItem permissionsAllowedMetaAnnotationItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer) {
        Map<MethodInfo, AnnotationInstance> methodToInstanceCollector = new HashMap<>();
        Map<ClassInfo, AnnotationInstance> classAnnotations = new HashMap<>();
        Map<MethodInfo, SecurityCheck> result = new HashMap<>();
        var permitAllGatherer = new SecurityAnnotationGatherer(index.getAnnotations(PERMIT_ALL), methodToInstanceCollector,
                ((m, i) -> result.put(m, recorder.permitAll())), classAnnotations, hasAdditionalSecurityAnnotations);
        var authenticatedGatherer = new SecurityAnnotationGatherer(index.getAnnotations(DotNames.AUTHENTICATED),
                methodToInstanceCollector, ((m, i) -> result.put(m, recorder.authenticated())), classAnnotations,
                hasAdditionalSecurityAnnotations);
        var denyAllGatherer = new SecurityAnnotationGatherer(index.getAnnotations(DENY_ALL), methodToInstanceCollector,
                ((m, i) -> result.put(m, recorder.denyAll())), classAnnotations, hasAdditionalSecurityAnnotations);
        // here we just collect all methods annotated with @RolesAllowed
        Map<MethodInfo, String[]> methodToRoles = new HashMap<>();
        var rolesAllowedGatherer = new SecurityAnnotationGatherer(index.getAnnotations(ROLES_ALLOWED),
                methodToInstanceCollector,
                ((methodInfo, instance) -> methodToRoles.put(methodInfo, instance.value().asStringArray())), classAnnotations,
                hasAdditionalSecurityAnnotations);

        // gather method-level instances for @Authenticated, @RolesAllowed, @PermitAll, @DenyAll
        permitAllGatherer.gatherMethodSecurityAnnotations();
        authenticatedGatherer.gatherMethodSecurityAnnotations();
        denyAllGatherer.gatherMethodSecurityAnnotations();
        rolesAllowedGatherer.gatherMethodSecurityAnnotations();

        // gather @PermissionsAllowed security checks
        final Map<DotName, SecurityCheck> classNameToPermCheck;
        List<AnnotationInstance> permissionInstances = getPermissionsAllowedInstances(index,
                permissionsAllowedMetaAnnotationItem);
        if (!permissionInstances.isEmpty()) {
            var additionalClassInstances = registerClassSecurityCheckBuildItems
                    .stream()
                    .filter(i -> PERMISSIONS_ALLOWED.equals(i.securityAnnotationInstance.name()))
                    .map(i -> i.securityAnnotationInstance)
                    .toList();
            var securityChecks = new PermissionSecurityChecksBuilder(recorder, generatedClassesProducer,
                    reflectiveClassesProducer, index)
                    .gatherPermissionsAllowedAnnotations(permissionInstances, methodToInstanceCollector, classAnnotations,
                            additionalClassInstances, hasAdditionalSecurityAnnotations)
                    .validatePermissionClasses(index)
                    .createPermissionPredicates()
                    .build();
            result.putAll(securityChecks.getMethodSecurityChecks());
            classNameToPermCheck = securityChecks.getClassNameSecurityChecks();

            // register used permission classes for reflection
            for (String permissionClass : securityChecks.permissionClasses()) {
                reflectiveClassBuildItemBuildProducer
                        .produce(ReflectiveClassBuildItem.builder(permissionClass).constructors().fields().methods().build());
                log.debugf("Register Permission class for reflection: %s", permissionClass);
            }
        } else {
            classNameToPermCheck = Map.of();
        }

        // gather class-level instances for @Authenticated, @RolesAllowed, @PermitAll, @DenyAll
        permitAllGatherer.gatherClassSecurityAnnotations();
        authenticatedGatherer.gatherClassSecurityAnnotations();
        denyAllGatherer.gatherClassSecurityAnnotations();
        rolesAllowedGatherer.gatherClassSecurityAnnotations();

        // validate additional annotations on class level are not accompanied by standard security annotations
        additionalSecurityAnnotationItems
                .stream()
                .map(AdditionalSecurityAnnotationBuildItem::getSecurityAnnotationName)
                .forEach(additionalSecAnnName -> index
                        .getAnnotations(additionalSecAnnName)
                        .stream()
                        .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
                        .map(ai -> ai.target().asClass())
                        .filter(SecurityTransformerUtils::hasSecurityAnnotation)
                        .findFirst()
                        .ifPresent(ci -> {
                            var securityAnnotation = findFirstStandardSecurityAnnotation(ci).get().name();
                            throw new RuntimeException("""
                                    Class '%s' is annotated with '%s' and '%s' security annotations,
                                    however security annotations cannot be combined.
                                    """.formatted(ci.name(), additionalSecAnnName, securityAnnotation));
                        }));

        /*
         * Handle additional secured methods by adding the denyAll/rolesAllowed check to all public non-static methods
         * that don't have same security annotations
         */
        for (AdditionalSecured additionalSecuredMethod : additionalSecuredMethods) {
            if (!isPublicNonStaticNonConstructor(additionalSecuredMethod.methodInfo)) {
                continue;
            }
            if (hasAdditionalSecurityAnnotations.test(additionalSecuredMethod.methodInfo)) {
                continue;
            }
            AnnotationInstance alreadyExistingInstance = methodToInstanceCollector.get(additionalSecuredMethod.methodInfo);
            if (additionalSecuredMethod.rolesAllowed.isPresent()) {
                if (alreadyExistingInstance == null) {
                    methodToRoles.put(additionalSecuredMethod.methodInfo,
                            additionalSecuredMethod.rolesAllowed.get().toArray(String[]::new));
                } else if (alreadyHasAnnotation(alreadyExistingInstance, ROLES_ALLOWED)) {
                    // we should not try to add second @RolesAllowed
                    throw new IllegalStateException("Method " + additionalSecuredMethod.methodInfo.declaringClass() + "#"
                            + additionalSecuredMethod.methodInfo.name() + " should not have been added as an additional "
                            + "secured method as it's already annotated with @RolesAllowed.");
                }
            } else {
                if (alreadyExistingInstance == null) {
                    result.put(additionalSecuredMethod.methodInfo, recorder.denyAll());
                } else if (alreadyHasAnnotation(alreadyExistingInstance, DENY_ALL)) {
                    // we should not try to add second @DenyAll
                    throw new IllegalStateException("Method " + additionalSecuredMethod.methodInfo.declaringClass() + "#"
                            + additionalSecuredMethod.methodInfo.name() + " should not have been added as an additional "
                            + "secured method as it's already annotated with @DenyAll.");
                }
            }
        }

        // create roles allowed security checks
        // we create only one security check for each role set
        Map<Set<String>, SecurityCheck> cache = new HashMap<>();
        final AtomicInteger keyIndex = new AtomicInteger(0);
        final AtomicBoolean hasRolesAllowedCheckWithConfigExp = new AtomicBoolean(false);
        for (Map.Entry<MethodInfo, String[]> entry : methodToRoles.entrySet()) {
            final MethodInfo methodInfo = entry.getKey();
            result.put(methodInfo,
                    computeRolesAllowedCheck(cache, hasRolesAllowedCheckWithConfigExp, keyIndex, recorder, entry.getValue()));
        }

        if (!registerClassSecurityCheckBuildItems.isEmpty()) {
            var classStorageBuilder = new ClassStorageBuilder();
            registerClassSecurityCheckBuildItems.forEach(item -> {
                var securityAnnotationName = item.securityAnnotationInstance.name();

                final SecurityCheck securityCheck;
                if (DENY_ALL.equals(securityAnnotationName)) {
                    securityCheck = recorder.denyAll();
                } else if (PERMIT_ALL.equals(securityAnnotationName)) {
                    securityCheck = recorder.permitAll();
                } else if (AUTHENTICATED.equals(securityAnnotationName)) {
                    securityCheck = recorder.authenticated();
                } else if (ROLES_ALLOWED.equals(securityAnnotationName)) {
                    var allowedRoles = item.securityAnnotationInstance.value().asStringArray();
                    securityCheck = computeRolesAllowedCheck(cache, hasRolesAllowedCheckWithConfigExp, keyIndex, recorder,
                            allowedRoles);
                } else if (PERMISSIONS_ALLOWED.equals(securityAnnotationName)) {
                    securityCheck = Objects.requireNonNull(classNameToPermCheck.get(item.className));
                } else {
                    throw new IllegalStateException("Found unknown security annotation: " + securityAnnotationName);
                }

                classStorageBuilder.addSecurityCheck(item.className, securityCheck);
            });
            classSecurityCheckStorageProducer.produce(classStorageBuilder.build());
        }

        final boolean registerRolesAllowedConfigSource;
        // way to resolve roles allowed configuration expressions specified via annotations to configuration values
        if (!rolesAllowedConfigExpResolverBuildItems.isEmpty()) {
            registerRolesAllowedConfigSource = true;
            for (RolesAllowedConfigExpResolverBuildItem item : rolesAllowedConfigExpResolverBuildItems) {
                recorder.recordRolesAllowedConfigExpression(item.getRoleConfigExpr(), keyIndex.getAndIncrement(),
                        item.getConfigValueRecorder());
            }
        } else {
            registerRolesAllowedConfigSource = hasRolesAllowedCheckWithConfigExp.get();
        }

        if (hasRolesAllowedCheckWithConfigExp.get()) {
            // make sure config expressions are eagerly resolved inside security checks when app starts
            configExpSecurityCheckProducer
                    .produce(new ConfigExpRolesAllowedSecurityCheckBuildItem());
        }
        if (registerRolesAllowedConfigSource) {
            // register config source with the Config system
            configBuilderProducer
                    .produce(new RunTimeConfigBuilderBuildItem(QuarkusSecurityRolesAllowedConfigBuilder.class.getName()));
        }

        /*
         * If we need to add the denyAll security check to all unannotated methods, we simply go through all secured methods,
         * collect the declaring classes, then go through all methods of the classes and add the necessary check
         */
        if (denyUnannotated) {
            Set<ClassInfo> allClassesWithSecurityChecks = new HashSet<>(methodToInstanceCollector.keySet().size());
            for (MethodInfo methodInfo : methodToInstanceCollector.keySet()) {
                allClassesWithSecurityChecks.add(methodInfo.declaringClass());
            }
            for (ClassInfo classWithSecurityCheck : allClassesWithSecurityChecks) {
                for (MethodInfo methodInfo : classWithSecurityCheck.methods()) {
                    if (!isPublicNonStaticNonConstructor(methodInfo)) {
                        continue;
                    }
                    if (methodToInstanceCollector.containsKey(methodInfo)) { // the method already has a security check
                        continue;
                    }
                    if (hasAdditionalSecurityAnnotations.test(methodInfo)) {
                        continue;
                    }
                    result.put(methodInfo, recorder.denyAll());
                }
            }
        }

        return result;
    }

    private static SecurityCheck computeRolesAllowedCheck(Map<Set<String>, SecurityCheck> cache,
            AtomicBoolean hasRolesAllowedCheckWithConfigExp, AtomicInteger keyIndex, SecurityCheckRecorder recorder,
            String[] allowedRoles) {
        return cache.computeIfAbsent(getSetForKey(allowedRoles), new Function<Set<String>, SecurityCheck>() {
            @Override
            public SecurityCheck apply(Set<String> allowedRolesSet) {
                final int[] configExpressionPositions = configExpressionPositions(allowedRoles);
                if (configExpressionPositions.length > 0) {
                    // we need to use supplier check as security checks are created during static init
                    // while config expressions are resolved during runtime
                    hasRolesAllowedCheckWithConfigExp.set(true);

                    // we don't create security check for each method, therefore we need artificial keys
                    // we can safely use numbers as RolesAllowed config source prefix all keys
                    final int[] configKeys = new int[configExpressionPositions.length];
                    for (int i = 0; i < configExpressionPositions.length; i++) {
                        // now we just collect artificial keys, but
                        // before we add the property to the Config system, we prefix it, e.g.
                        // @RolesAllowed("${admin}") -> QuarkusSecurityRolesAllowedConfigSource.property-0=${admin}
                        configKeys[i] = keyIndex.getAndIncrement();
                    }
                    return recorder.rolesAllowedSupplier(allowedRoles, configExpressionPositions, configKeys);
                }
                return recorder.rolesAllowed(allowedRoles);
            }
        });
    }

    public static int[] configExpressionPositions(String[] allowedRoles) {
        final Set<Integer> expPositions = new HashSet<>();
        for (int i = 0; i < allowedRoles.length; i++) {
            final int exprStart = allowedRoles[i].indexOf("${");
            if (exprStart >= 0 && allowedRoles[i].indexOf('}', exprStart + 2) > 0) {
                expPositions.add(i);
            }
        }

        if (expPositions.isEmpty()) {
            return new int[0];
        }
        return expPositions.stream().mapToInt(Integer::intValue).toArray();
    }

    private static Set<String> getSetForKey(String[] allowedRoles) {
        if (allowedRoles.length == 0) { // shouldn't happen, but let's be on the safe side
            return Collections.emptySet();
        } else if (allowedRoles.length == 1) {
            return Collections.singleton(allowedRoles[0]);
        }
        // use a set in order to avoid caring about the order of elements
        return new HashSet<>(Arrays.asList(allowedRoles));
    }

    private static boolean alreadyHasAnnotation(AnnotationInstance alreadyExistingInstance, DotName annotationName) {
        return alreadyExistingInstance.target().kind() == AnnotationTarget.Kind.METHOD
                && alreadyExistingInstance.name().equals(annotationName);
    }

    static boolean isPublicNonStaticNonConstructor(MethodInfo methodInfo) {
        return Modifier.isPublic(methodInfo.flags()) && !Modifier.isStatic(methodInfo.flags())
                && !"<init>".equals(methodInfo.name());
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY);
    }

    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> beans) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityIdentityAssociation.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(IdentityProviderManagerCreator.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityIdentityProxy.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(X509IdentityProvider.class));
    }

    @BuildStep
    AdditionalBeanBuildItem authorizationController(LaunchModeBuildItem launchMode) {
        Class<? extends AuthorizationController> controllerClass = AuthorizationController.class;
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT && !security.authorizationEnabledInDevMode()) {
            controllerClass = DevModeDisabledAuthorizationController.class;
        }
        return AdditionalBeanBuildItem.builder().addBeanClass(controllerClass).build();
    }

    @BuildStep
    void validateStartUpObserversNotSecured(SynthesisFinishedBuildItem synthesisFinished,
            ValidationPhaseBuildItem validationPhase,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<ValidationErrorBuildItem> validationErrorProducer) {
        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        synthesisFinished
                .getObservers()
                .stream()
                .map(ObserverInfo::asObserver)
                .filter(observer -> observer.getObservedType().name().equals(STARTUP_EVENT_NAME))
                .map(ObserverInfo::getObserverMethod)
                .filter(Objects::nonNull) // synthetic observer method created for @Startup is null and not secured
                .forEach(mi -> {
                    if (hasSecurityAnnotation(annotationStore.getAnnotations(mi))
                            || hasClassLevelStandardSecurityAnnotation(mi, annotationStore)) {
                        var declaringClass = mi.declaringClass();
                        findFirstStandardSecurityAnnotation(annotationStore.getAnnotations(mi))
                                .or(() -> findFirstStandardSecurityAnnotation(
                                        annotationStore.getAnnotations(declaringClass)))
                                .map(AnnotationInstance::name)
                                .filter(name -> !name.equals(PERMIT_ALL))
                                .ifPresent(securityAnnotation -> {
                                    var errorMsg = String.format(
                                            "Method '%s#%s' cannot observe '%s' as the method is secured with the '%s' annotation",
                                            declaringClass.name(), mi.name(), STARTUP_EVENT_NAME, securityAnnotation);
                                    validationErrorProducer
                                            .produce(new ValidationErrorBuildItem(new ConfigurationException(errorMsg)));
                                });
                    }
                });
    }

    @BuildStep
    void gatherClassSecurityChecks(BuildProducer<RegisterClassSecurityCheckBuildItem> producer,
            BeanArchiveIndexBuildItem indexBuildItem, PermissionsAllowedMetaAnnotationBuildItem permsMetaAnnotationsItem,
            List<ClassSecurityCheckAnnotationBuildItem> classAnnotationItems) {
        if (!classAnnotationItems.isEmpty()) {
            var index = indexBuildItem.getIndex();
            classAnnotationItems
                    .stream()
                    .map(ClassSecurityCheckAnnotationBuildItem::getClassAnnotation)
                    .map(index::getAnnotations)
                    .flatMap(Collection::stream)
                    .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
                    .map(ai -> ai.target().asClass())
                    .filter(cl -> SecurityTransformerUtils.hasSecurityAnnotation(cl)
                            || permsMetaAnnotationsItem.hasPermissionsAllowed(cl))
                    .map(c -> new RegisterClassSecurityCheckBuildItem(c.name(), findFirstStandardSecurityAnnotation(c)
                            .or(() -> permsMetaAnnotationsItem.findPermissionsAllowedInstance(c))
                            .get()))
                    .forEach(producer::produce);
        }
    }

    private static boolean hasClassLevelStandardSecurityAnnotation(MethodInfo method, AnnotationStore annotationStore) {
        return applyClassLevenInterceptor(method, annotationStore)
                && hasSecurityAnnotation(annotationStore.getAnnotations(method.declaringClass()));
    }

    private static boolean applyClassLevenInterceptor(MethodInfo method, AnnotationStore store) {
        // whether class-level business method interceptors (@AroundInvoke) are applied
        return !method.isConstructor() && Modifier.isPublic(method.flags())
                && !store.hasAnnotation(method, NO_CLASS_INTERCEPTORS);
    }

    static MethodDescription createMethodDescription(MethodInfo additionalSecuredMethod) {
        String[] paramTypes = new String[additionalSecuredMethod.parametersCount()];
        for (int i = 0; i < additionalSecuredMethod.parametersCount(); i++) {
            paramTypes[i] = additionalSecuredMethod.parameterTypes().get(i).name().toString();
        }
        return new MethodDescription(additionalSecuredMethod.declaringClass().name().toString(), additionalSecuredMethod.name(),
                paramTypes);
    }

    static class AdditionalSecured {

        final MethodInfo methodInfo;
        final Optional<List<String>> rolesAllowed;

        AdditionalSecured(MethodInfo methodInfo, Optional<List<String>> rolesAllowed) {
            this.methodInfo = methodInfo;
            this.rolesAllowed = rolesAllowed;
        }
    }

    static class SecurityCheckStorageAppPredicate implements Predicate<String> {

        @Override
        public boolean test(String s) {
            return s.equals(SecurityCheckStorage.class.getName());
        }
    }

    static final class MethodSecurityChecks extends SimpleBuildItem {
        Map<MethodInfo, SecurityCheck> securityChecks;

        MethodSecurityChecks(Map<MethodInfo, SecurityCheck> securityChecks) {
            this.securityChecks = securityChecks;
        }
    }

    private static Predicate<MethodInfo> hasAdditionalSecurityAnnotation(Set<DotName> additionalSecAnnotations) {
        return new Predicate<MethodInfo>() {
            @Override
            public boolean test(MethodInfo methodInfo) {
                return additionalSecAnnotations.stream().anyMatch(methodInfo::hasDeclaredAnnotation);
            }
        };
    }

    private static final class SecurityAnnotationGatherer {
        private final Collection<AnnotationInstance> annotationInstances;
        private final Map<MethodInfo, AnnotationInstance> alreadyCheckedMethods;
        private final BiConsumer<MethodInfo, AnnotationInstance> putResult;
        private final Map<ClassInfo, AnnotationInstance> classLevelAnnotations;
        private final Predicate<MethodInfo> hasAdditionalSecurityAnnotation;

        private SecurityAnnotationGatherer(Collection<AnnotationInstance> annotationInstances,
                Map<MethodInfo, AnnotationInstance> alreadyCheckedMethods, BiConsumer<MethodInfo, AnnotationInstance> putResult,
                Map<ClassInfo, AnnotationInstance> classLevelAnnotations,
                Predicate<MethodInfo> hasAdditionalSecurityAnnotation) {
            this.annotationInstances = annotationInstances;
            this.alreadyCheckedMethods = alreadyCheckedMethods;
            this.putResult = putResult;
            this.classLevelAnnotations = classLevelAnnotations;
            this.hasAdditionalSecurityAnnotation = hasAdditionalSecurityAnnotation;
        }

        private void gatherClassSecurityAnnotations() {
            // now add the class annotations to methods if they haven't already been annotated
            for (AnnotationInstance instance : annotationInstances) {
                AnnotationTarget target = instance.target();
                if (target.kind() == AnnotationTarget.Kind.CLASS) {
                    List<MethodInfo> methods = target.asClass().methods();
                    AnnotationInstance existingClassInstance = classLevelAnnotations.get(target.asClass());
                    if (existingClassInstance == null) {
                        classLevelAnnotations.put(target.asClass(), instance);
                        for (MethodInfo methodInfo : methods) {
                            AnnotationInstance alreadyExistingInstance = alreadyCheckedMethods.get(methodInfo);
                            if ((alreadyExistingInstance == null) && !hasAdditionalSecurityAnnotation.test(methodInfo)) {
                                putResult.accept(methodInfo, instance);
                            }
                        }
                    } else {
                        throw new IllegalStateException(
                                "Class " + target.asClass() + " is annotated with multiple security annotations "
                                        + instance.name()
                                        + " and " + existingClassInstance.name());
                    }
                }

            }
        }

        private void gatherMethodSecurityAnnotations() {
            // make sure we process annotations on methods first
            for (AnnotationInstance instance : annotationInstances) {
                AnnotationTarget target = instance.target();
                if (target.kind() == AnnotationTarget.Kind.METHOD) {
                    MethodInfo methodInfo = target.asMethod();
                    if (alreadyCheckedMethods.containsKey(methodInfo) || hasAdditionalSecurityAnnotation.test(methodInfo)) {
                        throw new IllegalStateException(
                                "Method " + methodInfo.name() + " of class " + methodInfo.declaringClass()
                                        + " is annotated with multiple security annotations");
                    }
                    alreadyCheckedMethods.put(methodInfo, instance);
                    putResult.accept(methodInfo, instance);
                }
            }
        }
    }
}
