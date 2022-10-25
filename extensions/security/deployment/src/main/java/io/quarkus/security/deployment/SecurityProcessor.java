package io.quarkus.security.deployment;

import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.security.runtime.SecurityProviderUtils.findProviderIndex;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedNativeImageClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JPMSExportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageSecurityProviderBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
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
import io.quarkus.security.runtime.IdentityProviderManagerCreator;
import io.quarkus.security.runtime.SecurityBuildTimeConfig;
import io.quarkus.security.runtime.SecurityCheckRecorder;
import io.quarkus.security.runtime.SecurityIdentityAssociation;
import io.quarkus.security.runtime.SecurityIdentityProxy;
import io.quarkus.security.runtime.SecurityProviderRecorder;
import io.quarkus.security.runtime.SecurityProviderUtils;
import io.quarkus.security.runtime.X509IdentityProvider;
import io.quarkus.security.runtime.interceptor.AuthenticatedInterceptor;
import io.quarkus.security.runtime.interceptor.DenyAllInterceptor;
import io.quarkus.security.runtime.interceptor.PermitAllInterceptor;
import io.quarkus.security.runtime.interceptor.RolesAllowedInterceptor;
import io.quarkus.security.runtime.interceptor.SecurityCheckStorageBuilder;
import io.quarkus.security.runtime.interceptor.SecurityConstrainer;
import io.quarkus.security.runtime.interceptor.SecurityHandler;
import io.quarkus.security.spi.AdditionalSecuredClassesBuildItem;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.DevModeDisabledAuthorizationController;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;

public class SecurityProcessor {

    private static final Logger log = Logger.getLogger(SecurityProcessor.class);

    SecurityConfig security;

    /**
     * Create JCAProviderBuildItems for any configured provider names
     */
    @BuildStep
    void produceJcaSecurityProviders(BuildProducer<JCAProviderBuildItem> jcaProviders,
            BuildProducer<BouncyCastleProviderBuildItem> bouncyCastleProvider,
            BuildProducer<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProvider) {
        Set<String> providers = security.securityProviders.orElse(Set.of());
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
                jcaProviders.produce(new JCAProviderBuildItem(providerName, security.securityProviderConfig.get(providerName)));
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
                classes.produce(new ReflectiveClassBuildItem(true, true, className));
                log.debugf("Register JCA class: %s", className);
            }
        }
    }

    @BuildStep
    void prepareBouncyCastleProviders(BuildProducer<ReflectiveClassBuildItem> reflection,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReInitialized,
            List<BouncyCastleProviderBuildItem> bouncyCastleProviders,
            List<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProviders) throws Exception {
        Optional<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProvider = getOne(bouncyCastleJsseProviders);
        if (bouncyCastleJsseProvider.isPresent()) {
            reflection.produce(
                    new ReflectiveClassBuildItem(true, true, SecurityProviderUtils.BOUNCYCASTLE_JSSE_PROVIDER_CLASS_NAME));
            reflection.produce(new ReflectiveClassBuildItem(true, true, true,
                    "org.bouncycastle.jsse.provider.DefaultSSLContextSpi$LazyManagers"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem(
                            "org.bouncycastle.jsse.provider.DefaultSSLContextSpi$LazyManagers"));
            prepareBouncyCastleProvider(reflection, runtimeReInitialized, bouncyCastleJsseProvider.get().isInFipsMode());
        } else {
            Optional<BouncyCastleProviderBuildItem> bouncyCastleProvider = getOne(bouncyCastleProviders);
            if (bouncyCastleProvider.isPresent()) {
                prepareBouncyCastleProvider(reflection, runtimeReInitialized, bouncyCastleProvider.get().isInFipsMode());
            }
        }
    }

    private static void prepareBouncyCastleProvider(BuildProducer<ReflectiveClassBuildItem> reflection,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReInitialized,
            boolean isFipsMode) {
        reflection.produce(new ReflectiveClassBuildItem(true, true,
                isFipsMode ? SecurityProviderUtils.BOUNCYCASTLE_FIPS_PROVIDER_CLASS_NAME
                        : SecurityProviderUtils.BOUNCYCASTLE_PROVIDER_CLASS_NAME));
        reflection.produce(new ReflectiveClassBuildItem(true, true,
                "org.bouncycastle.jcajce.provider.symmetric.AES",
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
                "org.bouncycastle.jcajce.provider.asymmetric.rsa.PSSSignatureSpi$SHA256withRSA"));
        runtimeReInitialized
                .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.crypto.CryptoServicesRegistrar"));
        if (!isFipsMode) {
            reflection.produce(new ReflectiveClassBuildItem(true, true, true,
                    "org.bouncycastle.jcajce.provider.drbg.DRBG$Default"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.jcajce.provider.drbg.DRBG$Default"));
            runtimeReInitialized
                    .produce(new RuntimeReinitializedClassBuildItem("org.bouncycastle.jcajce.provider.drbg.DRBG$NonceAndIV"));
        } else {
            reflection.produce(new ReflectiveClassBuildItem(true, true, true, "org.bouncycastle.crypto.general.AES"));
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

    private <BI extends MultiBuildItem> Optional<BI> getOne(List<BI> items) {
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
    private List<String> registerProvider(String providerName,
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

    @BuildStep
    void registerSecurityInterceptors(BuildProducer<InterceptorBindingRegistrarBuildItem> registrars,
            BuildProducer<AdditionalBeanBuildItem> beans) {
        registrars.produce(new InterceptorBindingRegistrarBuildItem(new SecurityAnnotationsRegistrar()));
        Class<?>[] interceptors = { AuthenticatedInterceptor.class, DenyAllInterceptor.class, PermitAllInterceptor.class,
                RolesAllowedInterceptor.class };
        beans.produce(new AdditionalBeanBuildItem(interceptors));
        beans.produce(new AdditionalBeanBuildItem(SecurityHandler.class, SecurityConstrainer.class));
    }

    /*
     * The annotation store is not meant to be generally supported for security annotation.
     * It is only used here in order to be able to register the DenyAllInterceptor for
     * methods that don't have a security annotation
     */
    @BuildStep
    void transformSecurityAnnotations(BuildProducer<AnnotationsTransformerBuildItem> transformers,
            List<AdditionalSecuredClassesBuildItem> additionalSecuredClasses,
            SecurityBuildTimeConfig config) {
        if (config.denyUnannotated) {
            transformers.produce(new AnnotationsTransformerBuildItem(new DenyingUnannotatedTransformer()));
        }
        if (!additionalSecuredClasses.isEmpty()) {
            for (AdditionalSecuredClassesBuildItem securedClasses : additionalSecuredClasses) {
                Set<String> additionalSecured = new HashSet<>();
                for (ClassInfo additionalSecuredClass : securedClasses.additionalSecuredClasses) {
                    additionalSecured.add(additionalSecuredClass.name().toString());
                }
                if (securedClasses.rolesAllowed.isPresent()) {
                    transformers.produce(
                            new AnnotationsTransformerBuildItem(new AdditionalRolesAllowedTransformer(additionalSecured,
                                    securedClasses.rolesAllowed.get())));
                } else {
                    transformers.produce(
                            new AnnotationsTransformerBuildItem(
                                    new AdditionalDenyingUnannotatedTransformer(additionalSecured)));
                }
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void gatherSecurityChecks(BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BeanArchiveIndexBuildItem beanArchiveBuildItem,
            BuildProducer<ApplicationClassPredicateBuildItem> classPredicate,
            List<AdditionalSecuredClassesBuildItem> additionalSecuredClasses,
            SecurityCheckRecorder recorder,
            List<AdditionalSecurityCheckBuildItem> additionalSecurityChecks, SecurityBuildTimeConfig config) {
        classPredicate.produce(new ApplicationClassPredicateBuildItem(new SecurityCheckStorageAppPredicate()));

        final Map<DotName, AdditionalSecured> additionalSecured = new HashMap<>();
        for (AdditionalSecuredClassesBuildItem securedClasses : additionalSecuredClasses) {
            securedClasses.additionalSecuredClasses.forEach(c -> {
                if (!additionalSecured.containsKey(c.name())) {
                    additionalSecured.put(c.name(), new AdditionalSecured(c, securedClasses.rolesAllowed));
                }
            });
        }

        IndexView index = beanArchiveBuildItem.getIndex();
        Map<MethodInfo, SecurityCheck> securityChecks = gatherSecurityAnnotations(
                index, additionalSecured, config.denyUnannotated, recorder);
        for (AdditionalSecurityCheckBuildItem additionalSecurityCheck : additionalSecurityChecks) {
            securityChecks.put(additionalSecurityCheck.getMethodInfo(),
                    additionalSecurityCheck.getSecurityCheck());
        }

        RuntimeValue<SecurityCheckStorageBuilder> builder = recorder.newBuilder();
        for (Map.Entry<MethodInfo, SecurityCheck> methodEntry : securityChecks
                .entrySet()) {
            MethodInfo method = methodEntry.getKey();
            String[] params = new String[method.parametersCount()];
            for (int i = 0; i < method.parametersCount(); ++i) {
                params[i] = method.parameterType(i).name().toString();
            }
            recorder.addMethod(builder, method.declaringClass().name().toString(), method.name(), params,
                    methodEntry.getValue());
        }
        recorder.create(builder);

        syntheticBeans.produce(
                SyntheticBeanBuildItem.configure(SecurityCheckStorage.class)
                        .scope(ApplicationScoped.class)
                        .creator(creator -> {
                            ResultHandle ret = creator.invokeStaticMethod(MethodDescriptor.ofMethod(SecurityCheckRecorder.class,
                                    "getStorage", SecurityCheckStorage.class));
                            creator.returnValue(ret);
                        }).done());
    }

    private Map<MethodInfo, SecurityCheck> gatherSecurityAnnotations(
            IndexView index,
            Map<DotName, AdditionalSecured> additionalSecuredClasses, boolean denyUnannotated, SecurityCheckRecorder recorder) {

        Map<MethodInfo, AnnotationInstance> methodToInstanceCollector = new HashMap<>();
        Map<ClassInfo, AnnotationInstance> classAnnotations = new HashMap<>();
        Map<MethodInfo, SecurityCheck> result = new HashMap<>(gatherSecurityAnnotations(
                index, DotNames.ROLES_ALLOWED, methodToInstanceCollector, classAnnotations,
                (instance -> recorder.rolesAllowed(instance.value().asStringArray()))));
        result.putAll(gatherSecurityAnnotations(index, DotNames.PERMIT_ALL, methodToInstanceCollector, classAnnotations,
                (instance -> recorder.permitAll())));
        result.putAll(gatherSecurityAnnotations(index, DotNames.AUTHENTICATED, methodToInstanceCollector, classAnnotations,
                (instance -> recorder.authenticated())));

        result.putAll(gatherSecurityAnnotations(index, DotNames.DENY_ALL, methodToInstanceCollector, classAnnotations,
                (instance -> recorder.denyAll())));

        /*
         * Handle additional secured classes by adding the denyAll check to all public non-static methods
         * that don't have security annotations
         */
        for (Map.Entry<DotName, AdditionalSecured> additionalSecureClassInfo : additionalSecuredClasses.entrySet()) {
            for (MethodInfo methodInfo : additionalSecureClassInfo.getValue().classInfo.methods()) {
                if (!isPublicNonStaticNonConstructor(methodInfo)) {
                    continue;
                }
                AnnotationInstance alreadyExistingInstance = methodToInstanceCollector.get(methodInfo);
                if ((alreadyExistingInstance == null)) {
                    if (additionalSecureClassInfo.getValue().rolesAllowed.isPresent()) {
                        result.put(methodInfo, recorder
                                .rolesAllowed(additionalSecureClassInfo.getValue().rolesAllowed.get().toArray(String[]::new)));
                    } else {
                        result.put(methodInfo, recorder.denyAll());
                    }
                } else if (alreadyExistingInstance.target().kind() == AnnotationTarget.Kind.CLASS) {
                    throw new IllegalStateException("Class " + methodInfo.declaringClass()
                            + " should not have been added as an additional secured class");
                }
            }
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
                    result.put(methodInfo, recorder.denyAll());
                }
            }
        }

        return result;
    }

    private boolean isPublicNonStaticNonConstructor(MethodInfo methodInfo) {
        return Modifier.isPublic(methodInfo.flags()) && !Modifier.isStatic(methodInfo.flags())
                && !"<init>".equals(methodInfo.name());
    }

    private Map<MethodInfo, SecurityCheck> gatherSecurityAnnotations(
            IndexView index, DotName dotName,
            Map<MethodInfo, AnnotationInstance> alreadyCheckedMethods,
            Map<ClassInfo, AnnotationInstance> classLevelAnnotations,
            Function<AnnotationInstance, SecurityCheck> securityCheckInstanceCreator) {

        Map<MethodInfo, SecurityCheck> result = new HashMap<>();

        Collection<AnnotationInstance> instances = index.getAnnotations(dotName);
        // make sure we process annotations on methods first
        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.METHOD) {
                MethodInfo methodInfo = target.asMethod();
                if (alreadyCheckedMethods.containsKey(methodInfo)) {
                    throw new IllegalStateException("Method " + methodInfo.name() + " of class " + methodInfo.declaringClass()
                            + " is annotated with multiple security annotations");
                }
                alreadyCheckedMethods.put(methodInfo, instance);
                result.put(methodInfo, securityCheckInstanceCreator.apply(instance));
            }
        }
        // now add the class annotations to methods if they haven't already been annotated
        for (AnnotationInstance instance : instances) {
            AnnotationTarget target = instance.target();
            if (target.kind() == AnnotationTarget.Kind.CLASS) {
                List<MethodInfo> methods = target.asClass().methods();
                AnnotationInstance existingClassInstance = classLevelAnnotations.get(target.asClass());
                if (existingClassInstance == null) {
                    classLevelAnnotations.put(target.asClass(), instance);
                    for (MethodInfo methodInfo : methods) {
                        AnnotationInstance alreadyExistingInstance = alreadyCheckedMethods.get(methodInfo);
                        if ((alreadyExistingInstance == null)) {
                            result.put(methodInfo, securityCheckInstanceCreator.apply(instance));
                        }
                    }
                } else {
                    throw new IllegalStateException(
                            "Class " + target.asClass() + " is annotated with multiple security annotations " + instance.name()
                                    + " and " + existingClassInstance.name());
                }
            }

        }

        return result;
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
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT && !security.authorizationEnabledInDevMode) {
            controllerClass = DevModeDisabledAuthorizationController.class;
        }
        return AdditionalBeanBuildItem.builder().addBeanClass(controllerClass).build();
    }

    static class AdditionalSecured {

        final ClassInfo classInfo;
        final Optional<List<String>> rolesAllowed;

        AdditionalSecured(ClassInfo classInfo, Optional<List<String>> rolesAllowed) {
            this.classInfo = classInfo;
            this.rolesAllowed = rolesAllowed;
        }
    }

    class SecurityCheckStorageAppPredicate implements Predicate<String> {

        @Override
        public boolean test(String s) {
            return s.equals(SecurityCheckStorage.class.getName());
        }
    }
}
