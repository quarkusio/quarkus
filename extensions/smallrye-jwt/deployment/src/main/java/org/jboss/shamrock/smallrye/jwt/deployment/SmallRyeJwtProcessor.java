/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.smallrye.jwt.deployment;

import java.security.interfaces.RSAPublicKey;

import org.jboss.logging.Logger;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ObjectSubstitutionBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.security.AuthConfig;
import io.quarkus.security.AuthConfigBuildItem;
import io.quarkus.security.IdentityManagerBuildItem;
import io.quarkus.security.SecurityDomainBuildItem;
import io.quarkus.security.SecurityRealmBuildItem;
import io.quarkus.smallrye.jwt.runtime.ClaimValueProducer;
import io.quarkus.smallrye.jwt.runtime.CommonJwtProducer;
import io.quarkus.smallrye.jwt.runtime.JWTAuthContextInfoGroup;
import io.quarkus.smallrye.jwt.runtime.JsonValueProducer;
import io.quarkus.smallrye.jwt.runtime.PrincipalProducer;
import io.quarkus.smallrye.jwt.runtime.RawClaimTypeProducer;
import io.quarkus.smallrye.jwt.runtime.SmallRyeJwtTemplate;
import io.quarkus.smallrye.jwt.runtime.auth.ClaimAttributes;
import io.quarkus.smallrye.jwt.runtime.auth.ElytronJwtCallerPrincipal;
import io.quarkus.smallrye.jwt.runtime.auth.JWTAuthMethodExtension;
import io.quarkus.smallrye.jwt.runtime.auth.MpJwtValidator;
import io.quarkus.smallrye.jwt.runtime.auth.PublicKeyProxy;
import io.quarkus.smallrye.jwt.runtime.auth.PublicKeySubstitution;
import io.quarkus.undertow.ServletExtensionBuildItem;
import org.wildfly.security.auth.server.SecurityRealm;

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
        additionalBeans.produce(new AdditionalBeanBuildItem(JWTAuthContextInfoProvider.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(false, MpJwtValidator.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(false, JWTAuthMethodExtension.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(CommonJwtProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(RawClaimTypeProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(PrincipalProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(ClaimValueProducer.class));
        additionalBeans.produce(new AdditionalBeanBuildItem(JsonValueProducer.class));
    }

    /**
     * Register this extension as a MP-JWT feature
     * 
     * @return
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.MP_JWT);
    }

    /**
     * Configure a TokenSecurityRealm if enabled
     * 
     * @param template - jwt runtime template
     * @param securityRealm - producer used to register the TokenSecurityRealm
     * @param container - the BeanContainer for creating CDI beans
     * @param reflectiveClasses - producer to register classes for reflection
     * @return auth config item for the MP-JWT auth method and realm
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @SuppressWarnings({ "unchecked", "rawtypes" })
    AuthConfigBuildItem configureFileRealmAuthConfig(SmallRyeJwtTemplate template,
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
            // Have the runtime template create the TokenSecurityRealm and create the build item
            RuntimeValue<SecurityRealm> realm = template.createTokenRealm(container.getValue());
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
     * @param template - jwt runtime template
     * @param securityDomain - the previously created TokenSecurityRealm
     * @param identityManagerProducer - producer for the identity manager
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureIdentityManager(SmallRyeJwtTemplate template, SecurityDomainBuildItem securityDomain,
            BuildProducer<IdentityManagerBuildItem> identityManagerProducer) {
        IdentityManager identityManager = template.createIdentityManager(securityDomain.getSecurityDomain());
        identityManagerProducer.produce(new IdentityManagerBuildItem(identityManager));
    }

    /**
     * Register the MP-JWT authentication servlet extension
     * 
     * @param template - jwt runtime template
     * @param container - the BeanContainer for creating CDI beans
     * @return servlet extension build item
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    ServletExtensionBuildItem registerJwtAuthExtension(SmallRyeJwtTemplate template, BeanContainerBuildItem container) {
        log.debugf("registerJwtAuthExtension");
        ServletExtension authExt = template.createAuthExtension(config.authMechanism, container.getValue());
        ServletExtensionBuildItem sebi = new ServletExtensionBuildItem(authExt);
        return sebi;
    }
}
