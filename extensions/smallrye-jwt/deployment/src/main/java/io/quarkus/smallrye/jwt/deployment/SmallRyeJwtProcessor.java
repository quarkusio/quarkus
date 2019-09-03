package io.quarkus.smallrye.jwt.deployment;

import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.QuarkusConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.security.deployment.JCAProviderBuildItem;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMechanism;
import io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator;
import io.smallrye.jwt.auth.cdi.ClaimValueProducer;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;
import io.smallrye.jwt.config.JWTAuthContextInfoProvider;

/**
 * The deployment processor for MP-JWT applications
 */
class SmallRyeJwtProcessor {
    private static final Logger log = Logger.getLogger(SmallRyeJwtProcessor.class.getName());

    SmallryeJWTConfig config;

    /**
     * Register the CDI beans that are needed by the MP-JWT extension
     *
     * @param additionalBeans - producer for additional bean items
     */
    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        if (config.enabled) {
            AdditionalBeanBuildItem.Builder unremovable = AdditionalBeanBuildItem.builder().setUnremovable();
            unremovable.addBeanClass(MpJwtValidator.class);
            unremovable.addBeanClass(JWTAuthMechanism.class);
            unremovable.addBeanClass(ClaimValueProducer.class);
            additionalBeans.produce(unremovable.build());
        }
        AdditionalBeanBuildItem.Builder removable = AdditionalBeanBuildItem.builder();
        removable.addBeanClass(JWTAuthContextInfoProvider.class);
        removable.addBeanClass(CommonJwtProducer.class);
        removable.addBeanClass(RawClaimTypeProducer.class);
        removable.addBeanClass(JsonValueProducer.class);
        removable.addBeanClass(JwtPrincipalProducer.class);
        additionalBeans.produce(removable.build());

    }

    /**
     * Register this extension as a MP-JWT feature
     *
     * @return FeatureBuildItem
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SMALLRYE_JWT);
    }

    /**
     * If the configuration specified a deployment local key resource, register it with substrate
     *
     * @return SubstrateResourceBuildItem
     */
    @BuildStep
    SubstrateResourceBuildItem registerSubstrateResources() {
        String publicKeyLocation = QuarkusConfig.getString("mp.jwt.verify.publickey.location", null, true);
        if (publicKeyLocation != null) {
            if (publicKeyLocation.indexOf(':') < 0 || publicKeyLocation.startsWith("classpath:")) {
                log.infof("Adding %s to native image", publicKeyLocation);
                return new SubstrateResourceBuildItem(publicKeyLocation);
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
}
