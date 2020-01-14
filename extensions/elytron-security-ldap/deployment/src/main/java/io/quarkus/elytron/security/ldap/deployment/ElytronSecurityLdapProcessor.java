package io.quarkus.elytron.security.ldap.deployment;

import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.elytron.security.deployment.ElytronPasswordMarkerBuildItem;
import io.quarkus.elytron.security.deployment.SecurityRealmBuildItem;
import io.quarkus.elytron.security.ldap.LdapRecorder;
import io.quarkus.elytron.security.ldap.config.LdapSecurityRealmConfig;
import io.quarkus.runtime.RuntimeValue;

class ElytronSecurityLdapProcessor {

    LdapSecurityRealmConfig ldap;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.SECURITY_ELYTRON_LDAP);
    }

    @BuildStep()
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.SECURITY_LDAP);
    }

    /**
     * Check to see if a LdapRealmConfig was specified and enabled and create a
     * {@linkplain org.wildfly.security.auth.realm.ldap.LdapSecurityRealm}
     *
     * @param recorder - runtime security recorder
     * @param securityRealm - the producer factory for the SecurityRealmBuildItem
     * @throws Exception - on any failure
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureLdapRealmAuthConfig(LdapRecorder recorder,
            BuildProducer<SecurityRealmBuildItem> securityRealm,
            BeanContainerBuildItem beanContainerBuildItem //we need this to make sure ArC is initialized
    ) throws Exception {
        if (ldap.enabled) {
            RuntimeValue<SecurityRealm> realm = recorder.createRealm(ldap);
            securityRealm.produce(new SecurityRealmBuildItem(realm, ldap.realmName, null));
        }
    }

    @BuildStep
    ElytronPasswordMarkerBuildItem marker() {
        if (ldap.enabled) {
            return new ElytronPasswordMarkerBuildItem();
        }
        return null;
    }

}
