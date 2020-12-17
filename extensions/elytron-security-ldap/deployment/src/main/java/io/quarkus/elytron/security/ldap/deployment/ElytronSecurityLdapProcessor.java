package io.quarkus.elytron.security.ldap.deployment;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.elytron.security.deployment.ElytronPasswordMarkerBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.ldap.LdapRecorder;
import io.quarkus.elytron.security.ldap.QuarkusDirContextFactory;
import io.quarkus.elytron.security.ldap.config.LdapSecurityRealmRuntimeConfig;
import io.quarkus.elytron.security.ldap.deployment.config.LdapSecurityRealmBuildTimeConfig;
import io.quarkus.runtime.RuntimeValue;

class ElytronSecurityLdapProcessor {

    @BuildStep()
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.SECURITY_LDAP);
    }

    /**
     * Check to see if a LdapRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.ldap.LdapSecurityRealm}
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureLdapRealmAuthConfig(LdapRecorder recorder,
            LdapSecurityRealmBuildTimeConfig ldapSecurityRealmBuildTimeConfig,
            LdapSecurityRealmRuntimeConfig ldapSecurityRealmRuntimeConfig,
            BuildProducer<SecurityRealmBuildItem> securityRealm,
            BeanContainerBuildItem beanContainerBuildItem //we need this to make sure ArC is initialized
    ) throws Exception {
        if (!ldapSecurityRealmBuildTimeConfig.enabled) {
            return;
        }

        RuntimeValue<SecurityRealm> realm = recorder.createRealm(ldapSecurityRealmRuntimeConfig);
        securityRealm.produce(new SecurityRealmBuildItem(realm, ldapSecurityRealmBuildTimeConfig.realmName, null));
    }

    @BuildStep
    ElytronPasswordMarkerBuildItem marker(LdapSecurityRealmBuildTimeConfig ldapSecurityRealmBuildTimeConfig) {
        if (!ldapSecurityRealmBuildTimeConfig.enabled) {
            return null;
        }
        return new ElytronPasswordMarkerBuildItem();
    }

    @BuildStep
    ReflectiveClassBuildItem enableReflection() {
        return new ReflectiveClassBuildItem(true, true, QuarkusDirContextFactory.INITIAL_CONTEXT_FACTORY);
    }
}
