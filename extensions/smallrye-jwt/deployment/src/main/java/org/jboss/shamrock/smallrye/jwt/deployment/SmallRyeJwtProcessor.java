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

package org.jboss.shamrock.smallrye.jwt.deployment;

import java.security.interfaces.RSAPublicKey;

import io.undertow.security.idm.IdentityManager;
import io.undertow.servlet.ServletExtension;
import org.jboss.logging.Logger;
import org.jboss.shamrock.arc.deployment.BeanContainerBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.ExecutionTime;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import io.smallrye.jwt.config.JWTAuthContextInfoProvider;
import org.jboss.shamrock.deployment.builditem.ObjectSubstitutionBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.runtime.RuntimeValue;
import org.jboss.shamrock.security.AuthConfig;
import org.jboss.shamrock.security.AuthConfigBuildItem;
import org.jboss.shamrock.security.IdentityManagerBuildItem;
import org.jboss.shamrock.security.SecurityDomainBuildItem;
import org.jboss.shamrock.security.SecurityRealmBuildItem;
import org.jboss.shamrock.smallrye.jwt.runtime.ClaimValueProducer;
import org.jboss.shamrock.smallrye.jwt.runtime.CommonJwtProducer;
import org.jboss.shamrock.smallrye.jwt.runtime.JWTAuthContextInfoGroup;
import org.jboss.shamrock.smallrye.jwt.runtime.JsonValueProducer;
import org.jboss.shamrock.smallrye.jwt.runtime.SmallRyeJwtTemplate;
import org.jboss.shamrock.smallrye.jwt.runtime.PrincipalProducer;
import org.jboss.shamrock.smallrye.jwt.runtime.RawClaimTypeProducer;
import org.jboss.shamrock.smallrye.jwt.runtime.auth.ClaimAttributes;
import org.jboss.shamrock.smallrye.jwt.runtime.auth.ElytronJwtCallerPrincipal;
import org.jboss.shamrock.smallrye.jwt.runtime.auth.JWTAuthMethodExtension;
import org.jboss.shamrock.smallrye.jwt.runtime.auth.MpJwtValidator;
import org.jboss.shamrock.smallrye.jwt.runtime.auth.PublicKeyProxy;
import org.jboss.shamrock.smallrye.jwt.runtime.auth.PublicKeySubstitution;
import org.jboss.shamrock.undertow.ServletExtensionBuildItem;
import org.wildfly.security.auth.server.SecurityRealm;

/**
 * The deployment processor for MP-JWT applications
 */
class SmallRyeJwtProcessor {
    private static final Logger log = Logger.getLogger(SmallRyeJwtProcessor.class.getName());

    JWTAuthContextInfoGroup config;

    /**
     * Register the CDI beans that are needed by the MP-JWT extension
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
     * @return
     */
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.MP_JWT);
    }

    /**
     * Configure a TokenSecurityRealm  if enabled
     * @param template - jwt runtime template
     * @param securityRealm - producer used to register the TokenSecurityRealm
     * @param container - the BeanContainer for creating CDI beans
     * @param reflectiveClasses - producer to register classes for reflection
     * @return auth config item for the MP-JWT auth method and realm
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    AuthConfigBuildItem configureFileRealmAuthConfig(SmallRyeJwtTemplate template,
                                                     BuildProducer<ObjectSubstitutionBuildItem> objectSubstitution,
                                                     BuildProducer<SecurityRealmBuildItem> securityRealm,
                                                     BeanContainerBuildItem container,
                                                     BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) throws Exception {
        if (config.enabled) {
            // RSAPublicKey needs to be serialized
            ObjectSubstitutionBuildItem.Holder pkHolder = new ObjectSubstitutionBuildItem.Holder(RSAPublicKey.class, PublicKeyProxy.class, PublicKeySubstitution.class);
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
