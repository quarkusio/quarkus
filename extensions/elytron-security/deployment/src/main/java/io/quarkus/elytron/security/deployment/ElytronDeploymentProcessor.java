package io.quarkus.elytron.security.deployment;

import java.util.List;

import org.jboss.logging.Logger;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.auth.server.SecurityRealm;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.elytron.security.runtime.DefaultRoleDecoder;
import io.quarkus.elytron.security.runtime.ElytronPasswordIdentityProvider;
import io.quarkus.elytron.security.runtime.ElytronRecorder;
import io.quarkus.elytron.security.runtime.ElytronSecurityDomainManager;
import io.quarkus.elytron.security.runtime.ElytronTokenIdentityProvider;
import io.quarkus.elytron.security.runtime.ElytronTrustedIdentityProvider;
import io.quarkus.runtime.RuntimeValue;

/**
 * The build time process for the security aspects of the deployment. This creates {@linkplain BuildStep}s for integration
 * with the Elytron security services. This supports the Elytron
 * {@linkplain org.wildfly.security.auth.realm.LegacyPropertiesSecurityRealm}
 * and {@linkplain org.wildfly.security.auth.realm.SimpleMapBackedSecurityRealm} realm implementations. Others could be
 * added by creating an extension that produces a SecurityRealmBuildItem for the realm.
 *
 * Additional authentication mechanisms can be added by producing AuthConfigBuildItems and including the associated
 * {@linkplain io.undertow.servlet.ServletExtension} implementations to register the
 * {@linkplain io.undertow.security.api.AuthenticationMechanismFactory}.
 *
 * TODO: The handling of the configuration to SecurityRealm instance creation/loading is clumsy to not being able to
 * have a config object annotated with @ConfigGroup inherit from another object with MP config annotated properties.
 *
 */
class ElytronDeploymentProcessor {
    private static final Logger log = Logger.getLogger(ElytronDeploymentProcessor.class.getName());

    /**
     * Register the Elytron-provided password factory SPI implementation
     *
     * @param classes producer factory for ReflectiveClassBuildItems
     */
    @BuildStep
    void services(BuildProducer<ReflectiveClassBuildItem> classes) {
        String[] allClasses = {
                "org.wildfly.security.password.impl.PasswordFactorySpiImpl",
        };
        classes.produce(new ReflectiveClassBuildItem(true, false, allClasses));
    }

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
     * Create the deployment SecurityDomain using the SecurityRealm build items that have been created.
     *
     * @param recorder - the runtime recorder class used to access runtime behaviors
     * @param realms - the previously created SecurityRealm runtime values
     * @return the SecurityDomain runtime value build item
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    SecurityDomainBuildItem build(ElytronRecorder recorder, List<SecurityRealmBuildItem> realms)
            throws Exception {
        if (realms.size() > 0) {
            // Configure the SecurityDomain.Builder from the main realm
            SecurityRealmBuildItem realmBuildItem = realms.get(0);
            RuntimeValue<SecurityDomain.Builder> securityDomainBuilder = recorder
                    .configureDomainBuilder(realmBuildItem.getName(), realmBuildItem.getRealm());
            // Add any additional SecurityRealms
            for (int n = 1; n < realms.size(); n++) {
                realmBuildItem = realms.get(n);
                RuntimeValue<SecurityRealm> realm = realmBuildItem.getRealm();
                recorder.addRealm(securityDomainBuilder, realmBuildItem.getName(), realm);
            }
            // Actually build the runtime value for the SecurityDomain
            RuntimeValue<SecurityDomain> securityDomain = recorder.buildDomain(securityDomainBuilder);

            // Return the build item for the SecurityDomain runtime value
            return new SecurityDomainBuildItem(securityDomain);
        }
        return null;
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void registerPasswordProvider(ElytronRecorder recorder, ShutdownContextBuildItem shutdownContextBuildItem) {
        recorder.registerPasswordProvider(shutdownContextBuildItem);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void identityManager(ElytronRecorder recorder, SecurityDomainBuildItem securityDomain, BeanContainerBuildItem bc) {
        if (securityDomain != null) {
            recorder.setDomainForIdentityProvider(bc.getValue(), securityDomain.getSecurityDomain());
        }
    }

    /**
     * For each SecurityRealm, load it's runtime state. This is currently a little strange due to how the AuthConfig is
     * downcast to the type of SecurityRealm configuration instance.
     *
     * @param recorder - the runtime recorder class used to access runtime behaviors
     * @param realms - the previously created SecurityRealm runtime values
     * @throws Exception
     */
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void loadRealm(ElytronRecorder recorder, List<SecurityRealmBuildItem> realms) throws Exception {
        for (SecurityRealmBuildItem realm : realms) {
            if (realm.getRuntimeLoadTask() != null) {
                recorder.runLoadTask(realm.getRuntimeLoadTask());
            }
        }
    }

}
