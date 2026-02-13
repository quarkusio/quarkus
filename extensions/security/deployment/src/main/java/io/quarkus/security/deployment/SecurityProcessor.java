package io.quarkus.security.deployment;

import static io.quarkus.arc.processor.DotNames.NO_CLASS_INTERCEPTORS;
import static io.quarkus.gizmo.MethodDescriptor.ofMethod;
import static io.quarkus.security.deployment.DotNames.AUTHENTICATED;
import static io.quarkus.security.deployment.DotNames.DENY_ALL;
import static io.quarkus.security.deployment.DotNames.INHERITED;
import static io.quarkus.security.deployment.DotNames.PERMISSIONS_ALLOWED;
import static io.quarkus.security.deployment.DotNames.PERMIT_ALL;
import static io.quarkus.security.deployment.DotNames.ROLES_ALLOWED;
import static io.quarkus.security.deployment.PermissionSecurityChecks.BLOCKING;
import static io.quarkus.security.deployment.PermissionSecurityChecks.PERMISSION_CHECKER_NAME;
import static io.quarkus.security.deployment.PermissionSecurityChecks.PermissionSecurityChecksBuilder.movePermFromMetaAnnToMetaTarget;
import static io.quarkus.security.runtime.SecurityProviderUtils.findProviderIndex;
import static io.quarkus.security.spi.SecurityTransformer.AuthorizationType.AUTHORIZATION_POLICY;
import static io.quarkus.security.spi.SecurityTransformer.AuthorizationType.SECURITY_CHECK;
import static io.quarkus.security.spi.SecurityTransformerBuildItem.createSecurityTransformer;

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
import java.util.EnumMap;
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

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTransformation;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.InterceptorBindingRegistrarBuildItem;
import io.quarkus.arc.deployment.SynthesisFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.AnnotationStore;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.InterceptorBindingRegistrar;
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
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationClassPredicateBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
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
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.execannotations.ExecutionModelAnnotationsAllowedBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeImageFutureDefault;
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
import io.quarkus.security.Authenticated;
import io.quarkus.security.PermissionsAllowed;
import io.quarkus.security.deployment.PermissionSecurityChecks.PermissionSecurityChecksBuilder;
import io.quarkus.security.identity.RunAsUser;
import io.quarkus.security.identity.SecurityIdentityAugmentor;
import io.quarkus.security.runtime.IdentityProviderManagerCreator;
import io.quarkus.security.runtime.PrincipalProducer;
import io.quarkus.security.runtime.QuarkusPermissionSecurityIdentityAugmentor;
import io.quarkus.security.runtime.QuarkusSecurityRolesAllowedConfigBuilder;
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
import io.quarkus.security.runtime.interceptor.RunAsUserInterceptor;
import io.quarkus.security.runtime.interceptor.SecurityCheckStorageBuilder;
import io.quarkus.security.runtime.interceptor.SecurityConstrainer;
import io.quarkus.security.runtime.interceptor.SecurityHandler;
import io.quarkus.security.spi.AdditionalSecuredMethodsBuildItem;
import io.quarkus.security.spi.AdditionalSecurityAnnotationBuildItem;
import io.quarkus.security.spi.AdditionalSecurityConstrainerEventPropsBuildItem;
import io.quarkus.security.spi.ClassSecurityAnnotationBuildItem;
import io.quarkus.security.spi.ClassSecurityCheckStorageBuildItem;
import io.quarkus.security.spi.ClassSecurityCheckStorageBuildItem.ClassStorageBuilder;
import io.quarkus.security.spi.CurrentIdentityAssociationClassBuildItem;
import io.quarkus.security.spi.DefaultSecurityCheckBuildItem;
import io.quarkus.security.spi.PermissionsAllowedMetaAnnotationBuildItem;
import io.quarkus.security.spi.RegisterClassSecurityCheckBuildItem;
import io.quarkus.security.spi.RolesAllowedConfigExpResolverBuildItem;
import io.quarkus.security.spi.RunAsUserPredicateBuildItem;
import io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem;
import io.quarkus.security.spi.SecurityTransformer;
import io.quarkus.security.spi.SecurityTransformer.AuthorizationType;
import io.quarkus.security.spi.SecurityTransformerBuildItem;
import io.quarkus.security.spi.runtime.AuthorizationController;
import io.quarkus.security.spi.runtime.BlockingSecurityExecutor;
import io.quarkus.security.spi.runtime.DevModeDisabledAuthorizationController;
import io.quarkus.security.spi.runtime.MethodDescription;
import io.quarkus.security.spi.runtime.SecurityCheck;
import io.quarkus.security.spi.runtime.SecurityCheckStorage;

public class SecurityProcessor {

    private static final Logger log = Logger.getLogger(SecurityProcessor.class);
    private static final DotName STARTUP_EVENT_NAME = DotName.createSimple(StartupEvent.class.getName());
    private static final Set<DotName> SECURITY_CHECK_ANNOTATIONS = Set.of(DotName.createSimple(RolesAllowed.class.getName()),
            DotName.createSimple(PermissionsAllowed.class.getName()),
            DotName.createSimple(PermissionsAllowed.List.class.getName()),
            DotName.createSimple(Authenticated.class.getName()),
            DotName.createSimple(DenyAll.class.getName()),
            DotName.createSimple(PermitAll.class.getName()));

    SecurityConfig security;

    @BuildStep
    SecurityTransformerBuildItem createSecurityTransformerBuildItem(
            List<SecuredInterfaceAnnotationBuildItem> securedInterfacePredicates,
            List<AdditionalSecurityAnnotationBuildItem> additionalSecurityAnnotationBuildItems) {
        // collect security annotations
        Map<AuthorizationType, Set<DotName>> authorizationTypeToSecurityAnnotations = new EnumMap<>(AuthorizationType.class);
        authorizationTypeToSecurityAnnotations.put(SECURITY_CHECK, new HashSet<>(SECURITY_CHECK_ANNOTATIONS));
        additionalSecurityAnnotationBuildItems.forEach(i -> authorizationTypeToSecurityAnnotations
                .computeIfAbsent(i.getAuthorizationType(), k -> new HashSet<>()).add(i.getSecurityAnnotationName()));

        Predicate<ClassInfo> isInterfaceWithTransformations = securedInterfacePredicates.stream()
                .map(SecuredInterfaceAnnotationBuildItem::getIsInterfaceWithTransformations)
                .reduce(Predicate::or)
                .orElse(null);
        Set<DotName> securedAnnotations = securedInterfacePredicates.stream()
                .map(SecuredInterfaceAnnotationBuildItem::getAnnotationName)
                .collect(Collectors.toSet());

        return new SecurityTransformerBuildItem(authorizationTypeToSecurityAnnotations, isInterfaceWithTransformations,
                securedAnnotations);
    }

    @BuildStep
    List<AdditionalIndexedClassesBuildItem> registerAdditionalIndexedClassesBuildItem(
            SecurityTransformerBuildItem securityTransformerBuildItem) {
        // we need the combined index to contain security annotations in order to check for repeatable annotations
        // (we do not hardcode here knowledge which annotation is repeatable and which one isn't, so we check all)
        return List
                .of(new AdditionalIndexedClassesBuildItem(securityTransformerBuildItem.getAllSecurityAnnotationNames()));
    }

    @BuildStep
    void secureInterfaceImplementations(SecurityTransformerBuildItem securityTransformerBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformerProducer) {
        SecurityTransformer securityTransformer = createSecurityTransformer(
                combinedIndexBuildItem.getIndex(), securityTransformerBuildItem);
        var annotationTransformations = securityTransformer.getInterfaceTransformations();
        if (annotationTransformations != null) {
            annotationTransformations
                    .forEach(i -> annotationsTransformerProducer.produce(new AnnotationsTransformerBuildItem(i)));
        }
    }

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

    @BuildStep(onlyIf = NativeImageFutureDefault.RunTimeInitializeSecurityProvider.class)
    void registerBouncyCastleReflection(CurateOutcomeBuildItem curateOutcomeBuildItem,
            BuildProducer<ReflectiveClassBuildItem> reflection) {
        if (curateOutcomeBuildItem.getApplicationModel().getDependencies().stream().anyMatch(
                x -> x.getGroupId().equals("org.bouncycastle") && x.getArtifactId().startsWith("bcprov-"))) {
            reflection.produce(ReflectiveClassBuildItem.builder("org.bouncycastle.jcajce.provider.symmetric.AES",
                    "org.bouncycastle.jcajce.provider.symmetric.AES$Mappings",
                    "org.bouncycastle.jcajce.provider.asymmetric.EC",
                    "org.bouncycastle.jcajce.provider.asymmetric.EC$Mappings",
                    "org.bouncycastle.jcajce.provider.asymmetric.RSA",
                    "org.bouncycastle.jcajce.provider.asymmetric.RSA$Mappings",
                    "org.bouncycastle.jcajce.provider.drbg.DRBG",
                    "org.bouncycastle.jcajce.provider.drbg.DRBG$Mappings").methods().fields()
                    .build());
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
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeReInitialized,
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
                    .produce(new RuntimeInitializedClassBuildItem(
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
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeReInitialized, boolean isFipsMode) {
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

        if (curateOutcomeBuildItem.getApplicationModel().getDependencies().stream().anyMatch(
                x -> x.getGroupId().equals("org.bouncycastle") && x.getArtifactId().startsWith("bcpkix-"))) {
            reflection.produce(
                    ReflectiveClassBuildItem.builder("org.bouncycastle.openssl.PEMParser").constructors(false).build());
        }

        runtimeReInitialized
                .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.crypto.CryptoServicesRegistrar"));
        if (!isFipsMode) {
            reflection.produce(ReflectiveClassBuildItem.builder("org.bouncycastle.jcajce.provider.drbg.DRBG$Default")
                    .methods().fields().build());
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.jcajce.provider.drbg.DRBG$Default"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.jcajce.provider.drbg.DRBG$NonceAndIV"));
            // URLSeededEntropySourceProvider.seedStream may contain a reference to a 'FileInputStream' which includes
            // references to FileDescriptors which aren't allowed in the image heap
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem(
                            "org.bouncycastle.jcajce.provider.drbg.DRBG$URLSeededEntropySourceProvider"));
        } else {
            reflection.produce(ReflectiveClassBuildItem.builder("org.bouncycastle.crypto.general.AES")
                    .methods().fields().build());
            runtimeReInitialized.produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.crypto.general.AES"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem(
                            "org.bouncycastle.crypto.asymmetric.NamedECDomainParameters"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.crypto.asymmetric.CustomNamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.asn1.ua.DSTU4145NamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.asn1.sec.SECNamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.asn1.cryptopro.ECGOST3410NamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.asn1.x9.X962NamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.asn1.x9.ECNamedCurveTable"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.asn1.anssi.ANSSINamedCurves"));
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.asn1.teletrust.TeleTrusTNamedCurves"));
            runtimeReInitialized.produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.jcajce.spec.ECUtil"));
            // start of BCFIPS 2.0
            // started thread during initialization
            runtimeReInitialized
                    .produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.crypto.util.dispose.DisposalDaemon"));
            // secure randoms
            runtimeReInitialized.produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.crypto.fips.FipsDRBG"));
            runtimeReInitialized.produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.crypto.fips.Utils"));
            // re-detect JNI library availability
            runtimeReInitialized.produce(new RuntimeInitializedClassBuildItem("org.bouncycastle.crypto.fips.NativeLoader"));
        }

        // Reinitialize class because it embeds a java.lang.ref.Cleaner instance in the image heap
        runtimeReInitialized.produce(new RuntimeInitializedClassBuildItem("sun.security.pkcs11.P11Util"));
    }

    @BuildStep(onlyIfNot = NativeImageFutureDefault.RunTimeInitializeSecurityProvider.class)
    @Record(ExecutionTime.STATIC_INIT)
    void recordBouncyCastleProvidersStaticInit(SecurityProviderRecorder recorder,
            List<BouncyCastleProviderBuildItem> bouncyCastleProviders,
            List<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProviders) {
        recordBouncyCastleProviders(recorder, bouncyCastleProviders, bouncyCastleJsseProviders);
    }

    @BuildStep(onlyIf = NativeImageFutureDefault.RunTimeInitializeSecurityProvider.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    void recordBouncyCastleProvidersRuntimeInit(SecurityProviderRecorder recorder,
            List<BouncyCastleProviderBuildItem> bouncyCastleProviders,
            List<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProviders) {
        recordBouncyCastleProviders(recorder, bouncyCastleProviders, bouncyCastleJsseProviders);
    }

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

    @BuildStep
    NativeImageFeatureBuildItem bouncyCastleFeature(NativeConfig nativeConfig,
            List<BouncyCastleProviderBuildItem> bouncyCastleProviders,
            List<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProviders) {

        if (nativeConfig.enabled()) {
            Optional<BouncyCastleJsseProviderBuildItem> bouncyCastleJsseProvider = getOne(bouncyCastleJsseProviders);
            Optional<BouncyCastleProviderBuildItem> bouncyCastleProvider = getOne(bouncyCastleProviders);

            if (bouncyCastleJsseProvider.isPresent() || bouncyCastleProvider.isPresent()) {
                return new NativeImageFeatureBuildItem("io.quarkus.security.BouncyCastleFeature");
            }
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

    /*
     * The annotation store is not meant to be generally supported for security annotation.
     * It is only used here in order to be able to register the DenyAllInterceptor for
     * methods that don't have a security annotation
     */
    @BuildStep
    void transformSecurityAnnotations(BuildProducer<AnnotationsTransformerBuildItem> transformers,
            List<AdditionalSecuredMethodsBuildItem> additionalSecuredMethods,
            SecurityTransformerBuildItem securityTransformerBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem) {
        if (security.denyUnannotatedMembers()) {
            SecurityTransformer securityTransformer = createSecurityTransformer(
                    combinedIndexBuildItem.getIndex(), securityTransformerBuildItem);
            transformers.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation
                    .forClasses()
                    .whenClass(new DenyUnannotatedPredicate(securityTransformer))
                    .transform(ctx -> ctx.add(DenyAll.class))));
        }
        if (!additionalSecuredMethods.isEmpty()) {
            for (AdditionalSecuredMethodsBuildItem securedMethods : additionalSecuredMethods) {
                Collection<MethodDescription> additionalSecured = new HashSet<>();
                for (MethodInfo additionalSecuredMethod : securedMethods.additionalSecuredMethods) {
                    additionalSecured.add(createMethodDescription(additionalSecuredMethod));
                }
                if (securedMethods.rolesAllowed.isPresent()) {
                    var additionalRolesAllowedTransformer = new AdditionalRolesAllowedTransformer(additionalSecured,
                            securedMethods.rolesAllowed.get());
                    transformers.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation
                            .forMethods()
                            .whenMethod(additionalRolesAllowedTransformer)
                            .transform(additionalRolesAllowedTransformer)));
                } else {
                    var additionalDenyingUnannotatedTransformer = new AdditionalDenyingUnannotatedTransformer(
                            additionalSecured);
                    transformers.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation
                            .forMethods()
                            .whenMethod(additionalDenyingUnannotatedTransformer)
                            .transform(additionalDenyingUnannotatedTransformer)));
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
            List<ClassSecurityAnnotationBuildItem> classAnnotationItems,
            SecurityTransformerBuildItem securityTransformerBuildItem) {

        var index = beanArchiveBuildItem.getIndex();
        var securityTransformer = createSecurityTransformer(index, securityTransformerBuildItem);
        var item = movePermFromMetaAnnToMetaTarget(securityTransformer);

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
                .map(ClassSecurityAnnotationBuildItem::getClassAnnotation).collect(Collectors.toSet());
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
    PermissionSecurityChecksBuilderBuildItem createPermissionSecurityChecksBuilder(
            BeanArchiveIndexBuildItem beanArchiveBuildItem,
            PermissionsAllowedMetaAnnotationBuildItem metaAnnotationItem,
            SecurityTransformerBuildItem securityTransformerBuildItem) {
        SecurityTransformer securityTransformer = createSecurityTransformer(beanArchiveBuildItem.getIndex(),
                securityTransformerBuildItem);
        return new PermissionSecurityChecksBuilderBuildItem(
                new PermissionSecurityChecksBuilder(beanArchiveBuildItem.getIndex(), metaAnnotationItem,
                        securityTransformer));
    }

    @BuildStep
    UnremovableBeanBuildItem makePermissionCheckerClassBeansUnremovable() {
        // this won't do the trick for checkers on abstract classes or beans producer fields and methods
        return new UnremovableBeanBuildItem(bi -> {
            if (bi.isRemovable() && bi.isClassBean()) {
                return bi.getTarget().map(t -> t.hasAnnotation(PERMISSION_CHECKER_NAME)).orElse(false);
            }
            return false;
        });
    }

    @BuildStep
    ExecutionModelAnnotationsAllowedBuildItem supportBlockingExecutionOfPermissionChecks() {
        return new ExecutionModelAnnotationsAllowedBuildItem(
                methodInfo -> methodInfo.hasDeclaredAnnotation(PERMISSION_CHECKER_NAME)
                        && methodInfo.hasDeclaredAnnotation(BLOCKING));
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void configurePermissionCheckers(PermissionSecurityChecksBuilderBuildItem checkerBuilder,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanProducer, SecurityCheckRecorder recorder,
            BeanDiscoveryFinishedBuildItem beanDiscoveryFinishedBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassProducer) {
        if (checkerBuilder.instance.foundPermissionChecker()) {

            // ==== produce SecurityIdentityAugmentor that checks QuarkusPermissions
            // why do we use synthetic bean?
            // - this processor relies on the bean archive index (cycle: idx -> additional bean -> idx)
            // - we have injection points (=> better validation from Arc) as checker beans are only requested from this augmentor
            var syntheticBeanConfigurator = SyntheticBeanBuildItem
                    .configure(QuarkusPermissionSecurityIdentityAugmentor.class)
                    .addType(SecurityIdentityAugmentor.class)
                    // ATM we do get augmentors from CDI once, no need to keep the instance in the CDI container
                    .scope(Dependent.class)
                    .unremovable()
                    .addInjectionPoint(Type.create(BlockingSecurityExecutor.class))
                    .createWith(recorder.createPermissionAugmentor());

            checkerBuilder.instance.getPermissionCheckers().stream().forEach(checkerMethod -> {
                var checkerClassType = Type.create(checkerMethod.declaringClass().name(), Type.Kind.CLASS);

                // validate permission checker method's declaring class is a CDI bean
                // synthetic beans are not taken into consideration which makes them not supported
                var matchingBeans = beanDiscoveryFinishedBuildItem.beanStream().assignableTo(checkerClassType).collect();
                if (matchingBeans.isEmpty()) {
                    throw new RuntimeException(
                            """
                                    @PermissionChecker declared on method '%s', but no matching CDI bean could be found for the declaring class '%s'.
                                    """
                                    .formatted(checkerMethod.name(), checkerClassType.name()));
                }
                // Using @Dependent is problematic because we would have to destroy beans manually at some point (which?)
                matchingBeans.stream().filter(b -> BuiltinScope.DEPENDENT.getInfo().equals(b.getScope())).findFirst()
                        .ifPresent(bi -> {
                            throw new RuntimeException(
                                    """
                                            Found @PermissionChecker annotation instance declared on the CDI bean method '%s#%s'.
                                            The CDI bean is a dependent scoped bean, but only the '@Singleton' bean or normal scoped beans are supported
                                            """
                                            .formatted(checkerMethod.name(), checkerClassType.name()));
                        });

                syntheticBeanConfigurator.addInjectionPoint(checkerClassType);
            });

            syntheticBeanProducer.produce(syntheticBeanConfigurator.done());

            // ==== Generate QuarkusPermission for each @PermissionChecker annotation instance
            checkerBuilder.instance.generatePermissionCheckers(generatedClassProducer);
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    MethodSecurityChecks gatherSecurityChecks(
            BuildProducer<ConfigExpRolesAllowedSecurityCheckBuildItem> configExpSecurityCheckProducer,
            List<RolesAllowedConfigExpResolverBuildItem> rolesAllowedConfigExpResolverBuildItems,
            BeanArchiveIndexBuildItem beanArchiveBuildItem,
            BuildProducer<RunTimeConfigBuilderBuildItem> configBuilderProducer,
            List<AdditionalSecuredMethodsBuildItem> additionalSecuredMethods,
            SecurityCheckRecorder recorder,
            BuildProducer<ClassSecurityCheckStorageBuildItem> classSecurityCheckStorageProducer,
            List<RegisterClassSecurityCheckBuildItem> registerClassSecurityCheckBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassBuildItemBuildProducer,
            List<AdditionalSecurityCheckBuildItem> additionalSecurityChecks,
            PermissionSecurityChecksBuilderBuildItem permissionSecurityChecksBuilderBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer,
            SecurityTransformerBuildItem securityTransformerBuildItem) {

        final Map<MethodDescription, AdditionalSecured> additionalSecured = new HashMap<>();
        for (AdditionalSecuredMethodsBuildItem securedMethods : additionalSecuredMethods) {
            for (MethodInfo m : securedMethods.additionalSecuredMethods) {
                additionalSecured.putIfAbsent(createMethodDescription(m),
                        new AdditionalSecured(m, securedMethods.rolesAllowed));
            }
        }

        IndexView index = beanArchiveBuildItem.getIndex();
        SecurityTransformer securityTransformer = createSecurityTransformer(index,
                securityTransformerBuildItem);
        Predicate<MethodInfo> hasAdditionalSecAnn = mi -> securityTransformer.hasSecurityAnnotation(mi,
                AUTHORIZATION_POLICY);
        Map<MethodInfo, SecurityCheck> securityChecks = gatherSecurityAnnotations(index, configExpSecurityCheckProducer,
                additionalSecured.values(), security.denyUnannotatedMembers(), recorder, configBuilderProducer,
                reflectiveClassBuildItemBuildProducer, rolesAllowedConfigExpResolverBuildItems,
                registerClassSecurityCheckBuildItems, classSecurityCheckStorageProducer, hasAdditionalSecAnn,
                permissionSecurityChecksBuilderBuildItem.instance,
                generatedClassesProducer, reflectiveClassesProducer, securityTransformer);
        for (AdditionalSecurityCheckBuildItem additionalSecurityCheck : additionalSecurityChecks) {
            securityChecks.put(additionalSecurityCheck.getMethodInfo(),
                    additionalSecurityCheck.getSecurityCheck());
        }
        Set<String> standardSecurityInterceptors = Set.of(DenyAllInterceptor.class.getName(),
                PermitAllInterceptor.class.getName(), RolesAllowedInterceptor.class.getName(),
                AuthenticatedInterceptor.class.getName(), PermissionsAllowedInterceptor.class.getName());

        securityChecks = securityChecks.entrySet()
                .stream()
                .filter(e -> {
                    MethodInfo methodInfo = e.getKey();
                    // constructors and standard security interceptors does not require security checks
                    return !("<init>".equals(methodInfo.name())
                            || standardSecurityInterceptors.contains(methodInfo.declaringClass().name().toString()));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

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
        syntheticBeans.produce(
                SyntheticBeanBuildItem.configure(SecurityCheckStorage.class)
                        .scope(ApplicationScoped.class)
                        .unremovable()
                        .runtimeProxy(recorder.create(builder))
                        .done());
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
            PermissionSecurityChecksBuilder permissionCheckBuilder,
            BuildProducer<GeneratedClassBuildItem> generatedClassesProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassesProducer,
            SecurityTransformer securityTransformer) {
        Map<MethodInfo, AnnotationInstance> methodToInstanceCollector = new HashMap<>();
        Map<ClassInfo, AnnotationInstance> classAnnotations = new HashMap<>();
        Map<MethodInfo, SecurityCheck> result = new HashMap<>();
        var permitAllGatherer = new SecurityAnnotationGatherer(securityTransformer.getAnnotations(PERMIT_ALL),
                methodToInstanceCollector,
                ((m, i) -> result.put(m, recorder.permitAll())), classAnnotations, hasAdditionalSecurityAnnotations);
        var authenticatedGatherer = new SecurityAnnotationGatherer(
                securityTransformer.getAnnotations(DotNames.AUTHENTICATED),
                methodToInstanceCollector, ((m, i) -> result.put(m, recorder.authenticated())), classAnnotations,
                hasAdditionalSecurityAnnotations);
        var denyAllGatherer = new SecurityAnnotationGatherer(securityTransformer.getAnnotations(DENY_ALL),
                methodToInstanceCollector,
                ((m, i) -> result.put(m, recorder.denyAll())), classAnnotations, hasAdditionalSecurityAnnotations);
        // here we just collect all methods annotated with @RolesAllowed
        Map<MethodInfo, String[]> methodToRoles = new HashMap<>();
        var rolesAllowedGatherer = new SecurityAnnotationGatherer(securityTransformer.getAnnotations(ROLES_ALLOWED),
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
        if (permissionCheckBuilder.foundPermissionsAllowedInstances()) {
            var additionalClassInstances = registerClassSecurityCheckBuildItems
                    .stream()
                    .filter(i -> PERMISSIONS_ALLOWED.equals(i.getSecurityAnnotationInstance().name()))
                    .map(RegisterClassSecurityCheckBuildItem::getSecurityAnnotationInstance)
                    .toList();
            var securityChecks = permissionCheckBuilder
                    .prepareParamConverterGenerator(recorder, generatedClassesProducer, reflectiveClassesProducer)
                    .gatherPermissionsAllowedAnnotations(methodToInstanceCollector, classAnnotations, additionalClassInstances,
                            hasAdditionalSecurityAnnotations)
                    .validatePermissionClasses()
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

        // we already validated that annotation target doesn't have more than one security check annotation
        // now validate that the same annotation target doesn't have both security check and authorization policy annotation
        securityTransformer.getSecurityAnnotationNames(AUTHORIZATION_POLICY)
                .forEach(additionalSecAnnName -> index
                        .getAnnotations(additionalSecAnnName)
                        .stream()
                        .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
                        .map(ai -> ai.target().asClass())
                        .filter(ai -> securityTransformer.hasSecurityAnnotation(ai, SECURITY_CHECK))
                        .findFirst()
                        .ifPresent(ci -> {
                            var securityAnnotation = securityTransformer.findFirstSecurityAnnotation(ci, SECURITY_CHECK)
                                    .get().name();
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
                var securityAnnotationName = item.getSecurityAnnotationInstance().name();

                final SecurityCheck securityCheck;
                if (DENY_ALL.equals(securityAnnotationName)) {
                    securityCheck = recorder.denyAll();
                } else if (PERMIT_ALL.equals(securityAnnotationName)) {
                    securityCheck = recorder.permitAll();
                } else if (AUTHENTICATED.equals(securityAnnotationName)) {
                    securityCheck = recorder.authenticated();
                } else if (ROLES_ALLOWED.equals(securityAnnotationName)) {
                    var allowedRoles = item.getSecurityAnnotationInstance().value().asStringArray();
                    securityCheck = computeRolesAllowedCheck(cache, hasRolesAllowedCheckWithConfigExp, keyIndex, recorder,
                            allowedRoles);
                } else if (PERMISSIONS_ALLOWED.equals(securityAnnotationName)) {
                    securityCheck = Objects.requireNonNull(classNameToPermCheck.get(item.getClassName()));
                } else {
                    throw new IllegalStateException("Found unknown security annotation: " + securityAnnotationName);
                }

                classStorageBuilder.addSecurityCheck(item.getClassName(), securityCheck);
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
        beans.produce(AdditionalBeanBuildItem.unremovableOf(PrincipalProducer.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(IdentityProviderManagerCreator.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(SecurityIdentityProxy.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(X509IdentityProvider.class));
    }

    @BuildStep
    AdditionalBeanBuildItem registerCurrentIdentityAssociationBean(
            Optional<CurrentIdentityAssociationClassBuildItem> currentIdentityAssociationClassBuildItem) {
        return currentIdentityAssociationClassBuildItem
                .map(CurrentIdentityAssociationClassBuildItem::getCurrentIdentityAssociationClass)
                .map(AdditionalBeanBuildItem::unremovableOf)
                .orElseGet(() -> AdditionalBeanBuildItem.unremovableOf(SecurityIdentityAssociation.class));
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
            BuildProducer<ValidationErrorBuildItem> validationErrorProducer,
            SecurityTransformerBuildItem securityTransformerBuildItem) {
        AnnotationStore annotationStore = validationPhase.getContext().get(BuildExtension.Key.ANNOTATION_STORE);
        SecurityTransformer securityTransformer = createSecurityTransformer(
                beanArchiveIndexBuildItem.getIndex(), securityTransformerBuildItem);
        synthesisFinished
                .getObservers()
                .stream()
                .map(ObserverInfo::asObserver)
                .filter(observer -> observer.getObservedType().name().equals(STARTUP_EVENT_NAME))
                .map(ObserverInfo::getObserverMethod)
                .filter(Objects::nonNull) // synthetic observer method created for @Startup is null and not secured
                .forEach(mi -> {
                    if (securityTransformer.isSecurityAnnotation(annotationStore.getAnnotations(mi))
                            || hasClassLevelStandardSecurityAnnotation(mi, annotationStore, securityTransformer)) {
                        var declaringClass = mi.declaringClass();
                        securityTransformer.findFirstSecurityAnnotation(annotationStore.getAnnotations(mi))
                                .or(() -> securityTransformer.findFirstSecurityAnnotation(
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
            List<ClassSecurityAnnotationBuildItem> classAnnotationItems,
            SecurityTransformerBuildItem securityTransformerBuildItem) {
        if (!classAnnotationItems.isEmpty()) {
            var index = indexBuildItem.getIndex();
            SecurityTransformer securityTransformer = createSecurityTransformer(index,
                    securityTransformerBuildItem);
            classAnnotationItems
                    .stream()
                    .map(ClassSecurityAnnotationBuildItem::getClassAnnotation)
                    .map(index::getAnnotations)
                    .flatMap(Collection::stream)
                    .filter(ai -> ai.target().kind() == AnnotationTarget.Kind.CLASS)
                    .map(ai -> ai.target().asClass())
                    .filter(cl -> securityTransformer.hasSecurityAnnotation(cl, SECURITY_CHECK)
                            || permsMetaAnnotationsItem.hasPermissionsAllowed(cl))
                    .map(c -> new RegisterClassSecurityCheckBuildItem(c.name(),
                            securityTransformer.findFirstSecurityAnnotation(c, SECURITY_CHECK)
                                    .or(() -> permsMetaAnnotationsItem.findPermissionsAllowedInstance(c))
                                    .get()))
                    .forEach(producer::produce);
        }
    }

    @BuildStep
    InterceptorBindingRegistrarBuildItem registerRunAsUserInterceptorBinding() {
        return new InterceptorBindingRegistrarBuildItem(new InterceptorBindingRegistrar() {
            @Override
            public List<InterceptorBinding> getAdditionalBindings() {
                return List.of(InterceptorBindingRegistrar.InterceptorBinding.of(RunAsUser.class, m -> true));
            }
        });
    }

    @BuildStep
    void registerRunAsUserInterceptorBean(BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformerProducer,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanProducer) {
        annotationsTransformerProducer.produce(new AnnotationsTransformerBuildItem(AnnotationTransformation
                .forClasses().whenClass(RunAsUserInterceptor.class)
                .transform(tc -> tc.add(AnnotationInstance.builder(RunAsUser.class).add("user", "").build()))));
        additionalBeanProducer.produce(AdditionalBeanBuildItem.unremovableOf(RunAsUserInterceptor.class));
    }

    @BuildStep
    void validateRunAsUserUsage(List<RunAsUserPredicateBuildItem> runAsUserPredicates,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem,
            BuildProducer<ValidationErrorBuildItem> errors) {
        var annotationInstances = beanArchiveIndexBuildItem.getIndex().getAnnotations(RunAsUser.class);
        if (annotationInstances.isEmpty()) {
            return;
        }

        var targetNotAllowedPredicate = Predicate.not(RunAsUserPredicateBuildItem.get(runAsUserPredicates));
        var notAllowedTargets = annotationInstances.stream()
                .map(AnnotationInstance::target)
                .filter(targetNotAllowedPredicate)
                .map(AnnotationTarget::asMethod)
                .map(SecurityProcessor::toString)
                .collect(Collectors.joining(", "));
        if (!notAllowedTargets.isEmpty()) {
            errors.produce(
                    new ValidationErrorBuildItem(new RuntimeException("Annotation '%s' cannot be used on following methods: %s"
                            .formatted(RunAsUser.class.getName(), notAllowedTargets))));
        }
    }

    private static String toString(MethodInfo mi) {
        return "%s#%s".formatted(mi.declaringClass().name().toString(), mi.name());
    }

    private static boolean hasClassLevelStandardSecurityAnnotation(MethodInfo method, AnnotationStore annotationStore,
            SecurityTransformer securityTransformer) {
        return applyClassLevenInterceptor(method, annotationStore)
                && securityTransformer.isSecurityAnnotation(annotationStore.getAnnotations(method.declaringClass()));
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
