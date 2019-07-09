package io.quarkus.smallrye.jwt.deployment;

import java.security.interfaces.RSAPublicKey;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.QuarkusConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.elytron.security.deployment.AuthConfigBuildItem;
import io.quarkus.elytron.security.deployment.IdentityManagerBuildItem;
import io.quarkus.elytron.security.deployment.JCAProviderBuildItem;
import io.quarkus.elytron.security.deployment.SecurityDomainBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.runtime.AuthConfig;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.smallrye.jwt.runtime.JWTAuthContextInfoGroup;
import io.quarkus.smallrye.jwt.runtime.SmallRyeJwtRecorder;
import io.quarkus.smallrye.jwt.runtime.auth.ClaimAttributes;
import io.quarkus.smallrye.jwt.runtime.auth.ElytronJwtCallerPrincipal;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMethodExtension;
import io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator;
import io.quarkus.smallrye.jwt.runtime.auth.PublicKeyProxy;
import io.quarkus.smallrye.jwt.runtime.auth.PublicKeySubstitution;
import io.quarkus.undertow.deployment.ServletExtensionBuildItem;
import io.smallrye.jwt.auth.cdi.ClaimValueProducer;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.PrincipalProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;
import io.smallrye.jwt.config.JWTAuthContextInfoProvider;
import io.undertow.security.idm.IdentityManager;
import io.undertow.servlet.ServletExtension;

/**
 * The deployment processor for MP-JWT applications
 */
class SmallRyeJwtProcessor {
    private static final Logger log = Logger.getLogger(SmallRyeJwtProcessor.class.getName());

    JWTAuthContextInfoGroup config;

    /**
     * Register the CDI beans that are needed by the MP-JWT extension
     *
     * @param additionalBeans - producer for additional bean items
     */
    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        AdditionalBeanBuildItem.Builder unremovable = AdditionalBeanBuildItem.builder().setUnremovable();
        unremovable.addBeanClass(MpJwtValidator.class);
        unremovable.addBeanClass(JWTAuthMethodExtension.class);
        additionalBeans.produce(unremovable.build());
        AdditionalBeanBuildItem.Builder removable = AdditionalBeanBuildItem.builder();
        removable.addBeanClass(JWTAuthContextInfoProvider.class);
        removable.addBeanClass(CommonJwtProducer.class);
        removable.addBeanClass(RawClaimTypeProducer.class);
        removable.addBeanClass(PrincipalProducer.class);
        removable.addBeanClass(ClaimValueProducer.class);
        removable.addBeanClass(JsonValueProducer.class);
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
     * Configure a TokenSecurityRealm if enabled
     *
     * @param recorder - jwt runtime recorder
     * @param securityRealm - producer used to register the TokenSecurityRealm
     * @param container - the BeanContainer for creating CDI beans
     * @param reflectiveClasses - producer to register classes for reflection
     * @return auth config item for the MP-JWT auth method and realm
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    AuthConfigBuildItem configureFileRealmAuthConfig(SmallRyeJwtRecorder recorder,
            BuildProducer<ObjectSubstitutionBuildItem> objectSubstitution,
            BuildProducer<SecurityRealmBuildItem> securityRealm,
            BeanContainerBuildItem container,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) throws Exception {
        if (config.enabled) {
            // RSAPublicKey needs to be serialized
            ObjectSubstitutionBuildItem.Holder pkHolder = new ObjectSubstitutionBuildItem.Holder(RSAPublicKey.class,
                    PublicKeyProxy.class, PublicKeySubstitution.class);
            ObjectSubstitutionBuildItem pkSub = new ObjectSubstitutionBuildItem(pkHolder);
            objectSubstitution.produce(pkSub);
            // Have the runtime recorder create the TokenSecurityRealm and create the build item
            RuntimeValue<SecurityRealm> realm = recorder.createTokenRealm(container.getValue());
            AuthConfig authConfig = new AuthConfig();
            authConfig.setAuthMechanism(config.authMechanism);
            authConfig.setRealmName(config.realmName);
            securityRealm.produce(new SecurityRealmBuildItem(realm, authConfig));

            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, ClaimAttributes.class.getName()));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, ElytronJwtCallerPrincipal.class.getName()));

            // Return the realm authentication mechanism build item
            return new AuthConfigBuildItem(authConfig);
        }
        return null;
    }

    /**
     * Create the JwtIdentityManager
     *
     * @param recorder - jwt runtime recorder
     * @param securityDomain - the previously created TokenSecurityRealm
     * @param identityManagerProducer - producer for the identity manager
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureIdentityManager(SmallRyeJwtRecorder recorder, SecurityDomainBuildItem securityDomain,
            BuildProducer<IdentityManagerBuildItem> identityManagerProducer) {
        if (config.enabled) {
            IdentityManager identityManager = recorder.createIdentityManager(securityDomain.getSecurityDomain());
            identityManagerProducer.produce(new IdentityManagerBuildItem(identityManager));
        }
    }

    /**
     * Register the MP-JWT authentication servlet extension
     *
     * @param recorder - jwt runtime recorder
     * @param container - the BeanContainer for creating CDI beans
     * @return servlet extension build item
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    ServletExtensionBuildItem registerJwtAuthExtension(SmallRyeJwtRecorder recorder, BeanContainerBuildItem container) {
        log.debugf("registerJwtAuthExtension");
        ServletExtension authExt = recorder.createAuthExtension(config.authMechanism, container.getValue());
        ServletExtensionBuildItem sebi = new ServletExtensionBuildItem(authExt);
        return sebi;
    }

    /**
     * Register the SHA256withRSA signature provider
     *
     * @return JCAProviderBuildItem for SHA256withRSA signature provider
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    JCAProviderBuildItem registerRSASigProvider() {
        return new JCAProviderBuildItem(config.rsaSigProvider);
    }
}
