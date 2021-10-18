package io.quarkus.smallrye.jwt.deployment;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.jwt.Claim;
import org.eclipse.microprofile.jwt.Claims;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem;
import io.quarkus.arc.deployment.BeanRegistrationPhaseBuildItem.BeanConfiguratorBuildItem;
import io.quarkus.arc.processor.BeanConfigurator;
import io.quarkus.arc.processor.BuildExtension;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.InjectionPointInfo;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.security.deployment.JCAProviderBuildItem;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMechanism;
import io.quarkus.smallrye.jwt.runtime.auth.JsonWebTokenCredentialProducer;
import io.quarkus.smallrye.jwt.runtime.auth.JwtPrincipalProducer;
import io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator;
import io.quarkus.smallrye.jwt.runtime.auth.RawOptionalClaimCreator;
import io.quarkus.vertx.http.deployment.SecurityInformationBuildItem;
import io.smallrye.jwt.algorithm.KeyEncryptionAlgorithm;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.auth.cdi.ClaimValueProducer;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JWTCallerPrincipalFactoryProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;
import io.smallrye.jwt.auth.principal.DefaultJWTParser;
import io.smallrye.jwt.config.JWTAuthContextInfoProvider;

/**
 * The deployment processor for MP-JWT applications
 */
class SmallRyeJwtProcessor {

    private static final Logger log = Logger.getLogger(SmallRyeJwtProcessor.class.getName());

    private static final DotName CLAIM_NAME = DotName.createSimple(Claim.class.getName());
    private static final DotName CLAIMS_NAME = DotName.createSimple(Claims.class.getName());

    SmallRyeJwtBuildTimeConfig config;

    @BuildStep(onlyIf = IsEnabled.class)
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.SMALLRYE_JWT);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @BuildStep(onlyIf = IsEnabled.class)
    public void provideSecurityInformation(BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {
        securityInformationProducer.produce(SecurityInformationBuildItem.JWT());
    }

    /**
     * Register the CDI beans that are needed by the MP-JWT extension
     *
     * @param additionalBeans - producer for additional bean items
     */
    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        if (config.enabled) {
            AdditionalBeanBuildItem.Builder unremovable = AdditionalBeanBuildItem.builder().setUnremovable();
            unremovable.addBeanClass(MpJwtValidator.class);
            unremovable.addBeanClass(JsonWebTokenCredentialProducer.class);
            unremovable.addBeanClass(JWTAuthMechanism.class);
            unremovable.addBeanClass(ClaimValueProducer.class);
            additionalBeans.produce(unremovable.build());
        }
        AdditionalBeanBuildItem.Builder removable = AdditionalBeanBuildItem.builder();
        removable.addBeanClass(JWTAuthContextInfoProvider.class);
        removable.addBeanClass(DefaultJWTParser.class);
        removable.addBeanClass(CommonJwtProducer.class);
        removable.addBeanClass(RawClaimTypeProducer.class);
        removable.addBeanClass(JsonValueProducer.class);
        removable.addBeanClass(JwtPrincipalProducer.class);
        removable.addBeanClass(JWTCallerPrincipalFactoryProducer.class);
        removable.addBeanClass(Claim.class);
        additionalBeans.produce(removable.build());

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, SignatureAlgorithm.class));
        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, KeyEncryptionAlgorithm.class));
    }

    /**
     * Register this extension as a MP-JWT feature
     *
     * @return FeatureBuildItem
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SMALLRYE_JWT);
    }

    /**
     * If the configuration specified a deployment local key resource, register it in native mode
     *
     * @return NativeImageResourceBuildItem
     */
    @BuildStep
    NativeImageResourceBuildItem registerNativeImageResources() {
        final Config config = ConfigProvider.getConfig();
        Optional<String> publicKeyLocationOpt = config.getOptionalValue("mp.jwt.verify.publickey.location", String.class);
        if (publicKeyLocationOpt.isPresent()) {
            final String publicKeyLocation = publicKeyLocationOpt.get();
            if (publicKeyLocation.indexOf(':') < 0 || publicKeyLocation.startsWith("classpath:")) {
                log.infof("Adding %s to native image", publicKeyLocation);
                return new NativeImageResourceBuildItem(publicKeyLocation);
            }
        }
        return null;
    }

    /**
     * Register the SHA256withRSA signature provider
     *
     * @return JCAProviderBuildItem for SHA256withRSA signature provider
     */
    @BuildStep
    JCAProviderBuildItem registerRSASigProvider() {
        return new JCAProviderBuildItem(config.rsaSigProvider);
    }

    @BuildStep
    void registerOptionalClaimProducer(BeanRegistrationPhaseBuildItem beanRegistrationPhase,
            BuildProducer<BeanConfiguratorBuildItem> beanConfigurator) {

        Set<Type> additionalTypes = new HashSet<>();

        // First analyze all relevant injection points
        for (InjectionPointInfo injectionPoint : beanRegistrationPhase.getContext().get(BuildExtension.Key.INJECTION_POINTS)) {
            if (injectionPoint.hasDefaultedQualifier()) {
                continue;
            }
            AnnotationInstance claimQualifier = injectionPoint.getRequiredQualifier(CLAIM_NAME);
            if (claimQualifier != null && injectionPoint.getType().name().equals(DotNames.PROVIDER)) {
                // Classes from javax.json are handled specially
                Type actualType = injectionPoint.getRequiredType();
                if (actualType.name().equals(DotNames.OPTIONAL) && !actualType.name().toString()
                        .startsWith("javax.json")) {
                    additionalTypes.add(actualType);
                }
            }
        }

        // Register a custom bean
        BeanConfigurator<Optional<?>> configurator = beanRegistrationPhase.getContext().configure(Optional.class);
        for (Type type : additionalTypes) {
            configurator.addType(type);
        }
        configurator.scope(BuiltinScope.DEPENDENT.getInfo());
        configurator.qualifiers(AnnotationInstance.create(CLAIM_NAME, null,
                new AnnotationValue[] { AnnotationValue.createStringValue("value", ""),
                        AnnotationValue.createEnumValue("standard", CLAIMS_NAME, "UNKNOWN") }));
        configurator.creator(RawOptionalClaimCreator.class);
        beanConfigurator.produce(new BeanConfiguratorBuildItem(configurator));
    }

    public static class IsEnabled implements BooleanSupplier {
        SmallRyeJwtBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
