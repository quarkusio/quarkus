package io.quarkus.elytron.security.ldap.deployment;

import java.util.function.BooleanSupplier;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.core.Phase;
import io.quarkus.core.deployment.service.ServiceRegistrar;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.AllowJNDIBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.elytron.security.deployment.ElytronPasswordMarkerBuildItem;
import io.quarkus.elytron.security.ldap.LdapRecorder;
import io.quarkus.elytron.security.ldap.QuarkusDirContextFactory;
import io.quarkus.elytron.security.ldap.config.LdapSecurityRealmRuntimeConfig;
import io.quarkus.elytron.security.ldap.deployment.config.LdapSecurityRealmBuildTimeConfig;

class ElytronSecurityLdapProcessor {

    @BuildStep()
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_LDAP);
    }

    @BuildStep(onlyIf = IsLdapSecurityEnabled.class)
    AllowJNDIBuildItem enableJndi() {
        //unfortunately we can't really use LDAP without JNDI
        return new AllowJNDIBuildItem();
    }

    /**
     * Check to see if a LdapRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.ldap.LdapSecurityRealm}
     */
    @BuildStep(onlyIf = IsLdapSecurityEnabled.class)
    void configureLdapRealmAuthConfig(ServiceRegistrar serviceRegistrar,
            LdapSecurityRealmBuildTimeConfig ldapSecurityRealmBuildTimeConfig) {
        serviceRegistrar.forService(SecurityRealm.class, ldapSecurityRealmBuildTimeConfig.realmName())
                .atPhase(Phase.DATA)
                .after(BeanContainer.class)
                .require(LdapSecurityRealmRuntimeConfig.class)
                .onStart((ctx, runtimeConfig) -> LdapRecorder.createRealm(runtimeConfig));
    }

    @BuildStep(onlyIf = IsLdapSecurityEnabled.class)
    ElytronPasswordMarkerBuildItem marker() {
        return new ElytronPasswordMarkerBuildItem();
    }

    @BuildStep(onlyIf = IsLdapSecurityEnabled.class)
    void registerForReflection(BuildProducer<ReflectiveClassBuildItem> reflection) {
        // All JDK provided InitialContextFactory impls via the module descriptors:
        // com.sun.jndi.ldap.LdapCtxFactory, com.sun.jndi.dns.DnsContextFactory and com.sun.jndi.rmi.registry.RegistryContextFactory
        reflection.produce(ReflectiveClassBuildItem.builder(QuarkusDirContextFactory.INITIAL_CONTEXT_FACTORY).methods()
                .fields().build());
        reflection.produce(
                ReflectiveClassBuildItem.builder("com.sun.jndi.dns.DnsContextFactory").build());
        reflection.produce(ReflectiveClassBuildItem.builder("com.sun.jndi.rmi.registry.RegistryContextFactory")
                .build());
    }

    static final class IsLdapSecurityEnabled implements BooleanSupplier {

        private final boolean enabled;

        IsLdapSecurityEnabled(LdapSecurityRealmBuildTimeConfig ldapSecurityRealmBuildTimeConfig) {
            this.enabled = ldapSecurityRealmBuildTimeConfig.enabled();
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }
}
