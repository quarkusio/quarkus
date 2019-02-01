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

package org.jboss.shamrock.jwt.deployment;

import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

import io.smallrye.jwt.auth.principal.JWTAuthContextInfo;
import io.undertow.security.idm.IdentityManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.ExecutionTime;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import io.smallrye.jwt.config.JWTAuthContextInfoProvider;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.recording.RecorderContext;
import org.jboss.shamrock.jwt.runtime.ClaimValueProducer;
import org.jboss.shamrock.jwt.runtime.JsonValueProducer;
import org.jboss.shamrock.jwt.runtime.JwtTemplate;
import org.jboss.shamrock.jwt.runtime.MPJWTProducer;
import org.jboss.shamrock.jwt.runtime.PrincipalProducer;
import org.jboss.shamrock.jwt.runtime.RawClaimTypeProducer;
import org.jboss.shamrock.jwt.runtime.auth.ClaimAttributes;
import org.jboss.shamrock.jwt.runtime.auth.ElytronJwtCallerPrincipal;
import org.jboss.shamrock.jwt.runtime.auth.JWTAuthMethodExtension;
import org.jboss.shamrock.jwt.runtime.auth.JWTAuthMethodExtensionProxy;
import org.jboss.shamrock.jwt.runtime.auth.JWTAuthMethodExtensionSubstitution;
import org.jboss.shamrock.jwt.runtime.auth.PublicKeyProxy;
import org.jboss.shamrock.jwt.runtime.auth.PublicKeySubstitution;
import org.jboss.shamrock.runtime.RuntimeValue;
import org.jboss.shamrock.security.AuthConfig;
import org.jboss.shamrock.security.AuthConfigBuildItem;
import org.jboss.shamrock.security.IdentityManagerBuildItem;
import org.jboss.shamrock.security.SecurityDomainBuildItem;
import org.jboss.shamrock.security.SecurityRealmBuildItem;
import org.jboss.shamrock.security.SecurityTemplate;
import org.jboss.shamrock.undertow.ServletExtensionBuildItem;
import org.wildfly.security.auth.server.SecurityRealm;


class JwtProcessor {
    private static final String NONE = "NONE";
    private static final Logger log = Logger.getLogger(JwtProcessor.class.getName());

    /**
     * The authentication mechanism
     */
    //@Inject
    @ConfigProperty(name = "shamrock.security.jwt.authMechanism", defaultValue = "MP-JWT")
    public String authMechanism;

    /**
     * The authentication mechanism
     */
    //@Inject
    @ConfigProperty(name = "shamrock.security.jwt.realmName", defaultValue = "Shamrock-JWT")
    public String realmName;

    /**
     * The MP-JWT configuration object
     */
    //@Inject
    @ConfigProperty(name = "shamrock.security.jwt.enabled", defaultValue = "false")
    public boolean enabled;

    // The MP-JWT spec defined configuration properties

    /**
     * @since 1.1
     */
    //@Inject
    @ConfigProperty(name = "mp.jwt.verify.publickey")
    public Optional<String> mpJwtublicKey;
    /**
     * @since 1.1
     */
    //@Inject
    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = NONE)
    public String mpJwtIssuer;
    /**
     * @since 1.1
     */
    //@Inject
    @ConfigProperty(name = "mp.jwt.verify.publickey.location")
    /**
     * @since 1.1
     */
    public Optional<String> mpJwtLocation;
    /**
     * Not part of the 1.1 release, but talked about.
     */
    //@Inject
    @ConfigProperty(name = "mp.jwt.verify.requireiss")
    public Optional<Boolean> mpJwtRequireIss;
    private JWTAuthContextInfoGroup jwtConfig = new JWTAuthContextInfoGroup();

    /**
     *
     * @param additionalBeans
     */
    @BuildStep
    void registerAdditionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(JWTAuthContextInfoProvider.class.getName()));
        additionalBeans.produce(new AdditionalBeanBuildItem(MPJWTProducer.class.getName()));
        additionalBeans.produce(new AdditionalBeanBuildItem(PrincipalProducer.class.getName()));
        additionalBeans.produce(new AdditionalBeanBuildItem(RawClaimTypeProducer.class.getName()));
        additionalBeans.produce(new AdditionalBeanBuildItem(ClaimValueProducer.class.getName()));
        additionalBeans.produce(new AdditionalBeanBuildItem(JsonValueProducer.class.getName()));

    }
    
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.MP_JWT);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    AuthConfigBuildItem configureFileRealmAuthConfig(JwtTemplate template, RecorderContext context,
                                                     BuildProducer<JWTAuthContextInfoBuildItem> jwtAuthContextInfoProducer,
                                                     BuildProducer<SecurityRealmBuildItem> securityRealm,
                                                     BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) throws Exception {
        if (enabled) {
            jwtConfig.mpJwtIssuer = mpJwtIssuer;
            jwtConfig.mpJwtLocation = mpJwtLocation;
            jwtConfig.mpJwtRequireIss = mpJwtRequireIss;
            jwtConfig.mpJwtublicKey = mpJwtublicKey;
            JWTAuthContextInfo jwtAuthContextInfo = jwtConfig.getContextInfo();
            jwtAuthContextInfoProducer.produce(new JWTAuthContextInfoBuildItem(jwtAuthContextInfo));

            context.registerSubstitution(RSAPublicKey.class, PublicKeyProxy.class, PublicKeySubstitution.class);

            // Have the runtime template create the TokenSecurityRealm and create the build item
            RuntimeValue<SecurityRealm> realm = template.createTokenRealm(jwtAuthContextInfo);
            AuthConfig authConfig = new AuthConfig();
            authConfig.setAuthMechanism(authMechanism);
            authConfig.setRealmName(realmName);
            securityRealm.produce(new SecurityRealmBuildItem(realm, authConfig));

            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, ClaimAttributes.class.getName()));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, ElytronJwtCallerPrincipal.class.getName()));

            // Return the realm authentication mechanism build item
            return new AuthConfigBuildItem(authConfig);
        }
        return null;
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void configureIdentityManager(JwtTemplate template, SecurityDomainBuildItem securityDomain,
                                  BuildProducer<IdentityManagerBuildItem> identityManagerProducer) {
        RuntimeValue<IdentityManager> identityManager = template.createIdentityManager(securityDomain.getSecurityDomain());
        identityManagerProducer.produce(new IdentityManagerBuildItem(identityManager));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    ServletExtensionBuildItem registerJwtAuthExtension(JwtTemplate template, RecorderContext context, JWTAuthContextInfoBuildItem contextInfo) {
        log.debugf("registerJwtAuthExtension");
        context.registerSubstitution(JWTAuthMethodExtension.class, JWTAuthMethodExtensionProxy.class, JWTAuthMethodExtensionSubstitution.class);
        JWTAuthContextInfo jwtAuthContextInfo = contextInfo.getJwtAuthContextInfo();
        ServletExtensionBuildItem sebi = new ServletExtensionBuildItem(new JWTAuthMethodExtension(authMechanism, jwtAuthContextInfo));
        sebi.addObjectSubstitution(PublicKeySubstitution.class);
        sebi.addObjectSubstitution(JWTAuthMethodExtensionSubstitution.class);
        return sebi;
    }
}
