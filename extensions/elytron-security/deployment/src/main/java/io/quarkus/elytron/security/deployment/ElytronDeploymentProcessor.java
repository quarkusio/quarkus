package io.quarkus.elytron.security.deployment;

import java.util.List;
import java.util.Optional;

import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.elytron.security.runtime.DefaultRoleDecoder;
import io.quarkus.elytron.security.runtime.ElytronPasswordIdentityProvider;
import io.quarkus.elytron.security.runtime.ElytronRecorder;
import io.quarkus.elytron.security.runtime.ElytronSecurityDomainManager;
import io.quarkus.elytron.security.runtime.ElytronSecurityRuntimeConfig;
import io.quarkus.elytron.security.runtime.ElytronTokenIdentityProvider;
import io.quarkus.elytron.security.runtime.ElytronTrustedIdentityProvider;

/**
 * The build time process for the security aspects of the deployment. This creates {@linkplain BuildStep}s for integration
 * with the Elytron security services. The {@linkplain SecurityRealm}s registered as named runtime services by the
 * Elytron realm extensions (properties file, JDBC, LDAP and OAuth2) are combined into a single
 * {@linkplain SecurityDomain}, which is then handed over to the Elytron identity providers.
 *
 * TODO: The handling of the configuration to SecurityRealm instance creation/loading is clumsy to not being able to
 * have a config object annotated with @ConfigGroup inherit from another object with MP config annotated properties.
 *
 */
class ElytronDeploymentProcessor {

    @BuildStep
    void addBeans(BuildProducer<AdditionalBeanBuildItem> beans, List<ElytronPasswordMarkerBuildItem> pw,
            List<ElytronTokenMarkerBuildItem> token) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(ElytronSecurityDomainManager.class));
        beans.produce(AdditionalBeanBuildItem.unremovableOf(DefaultRoleDecoder.class));
        if (!token.isEmpty()) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(ElytronTokenIdentityProvider.class));
        }
        if (!pw.isEmpty()) {
            beans.produce(AdditionalBeanBuildItem.unremovableOf(ElytronPasswordIdentityProvider.class));
            beans.produce(AdditionalBeanBuildItem.unremovableOf(ElytronTrustedIdentityProvider.class));
        }
    }

    /**
     * Create the deployment SecurityDomain using the SecurityRealms that have been registered as services.
     */
    @BuildStep
    void build(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar.forService(SecurityDomain.class)
                .atPhase(Phase.INIT)
                .consumeAll(SecurityRealm.class)
                .require(ElytronSecurityRuntimeConfig.class)
                .after(BeanContainer.class)
                .optional()
                .onStart((ctx, nameToSecurityRealm, config) -> {
                    if (nameToSecurityRealm.isEmpty()) {
                        return Optional.empty();
                    }

                    final String defaultName = ElytronRecorder.getDefaultRealmName(nameToSecurityRealm.keySet(), config);

                    final SecurityDomain.Builder securityDomainBuilder = ElytronRecorder.configureDomainBuilder(defaultName,
                            nameToSecurityRealm.get(defaultName));

                    nameToSecurityRealm.forEach((name, realm) -> {
                        if (!name.equals(defaultName)) {
                            ElytronRecorder.addRealm(securityDomainBuilder, name, realm);
                        }
                    });

                    return Optional.of(securityDomainBuilder.build());
                });
    }

    @BuildStep
    void setDomainForIdentityProvider(ServiceRegistrar serviceRegistrar) {
        serviceRegistrar.forService("io.quarkus.elytron-security.set-domain-for-identity-provider")
                .atPhase(Phase.INIT) // same phase as the SecurityDomain service, set as soon as the domain is available
                .require(BeanContainer.class)
                .request(SecurityDomain.class)
                // TODO: when Vert.x HTTP Security is migrated, we should migrate this 'before' to its init service
                //      until then, this is fine since the HTTP router socket is only opened after the startup event
                //      and this security domain is currently not needed before the first HTTP request
                .before("io.quarkus.arc.startup-event")
                .onStart((ctx, bc, securityDomain) -> securityDomain
                        .ifPresent(domain -> ElytronRecorder.setDomainForIdentityProvider(bc, domain)));
    }

}
