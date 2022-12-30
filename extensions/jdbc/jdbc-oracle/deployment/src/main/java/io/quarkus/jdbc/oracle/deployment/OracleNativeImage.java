package io.quarkus.jdbc.oracle.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class OracleNativeImage {

    /**
     * Registers the {@code oracle.jdbc.driver.OracleDriver} so that it can be loaded
     * by reflection, as commonly expected.
     */
    @BuildStep
    void reflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        // "oracle.jdbc.OracleDriver" is what's listed in the serviceloader resource from Oracle,
        // but it delegates all use to "oracle.jdbc.driver.OracleDriver" - which is also what's recommended by the docs.
        final String driverName = "oracle.jdbc.driver.OracleDriver";
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, driverName));

        // for ldap style jdbc urls. e.g. jdbc:oracle:thin:@ldap://oid:5000/mydb1,cn=OracleContext,dc=myco,dc=com
        //
        // Note that all JDK provided InitialContextFactory impls from the JDK registered via module descriptors
        // available at build time need to be reflectively accessible via ServiceLoader for runtime consistency.
        // These are:
        // com.sun.jndi.ldap.LdapCtxFactory, com.sun.jndi.dns.DnsContextFactory and com.sun.jndi.rmi.registry.RegistryContextFactory
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "com.sun.jndi.ldap.LdapCtxFactory"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "com.sun.jndi.dns.DnsContextFactory"));
        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false, "com.sun.jndi.rmi.registry.RegistryContextFactory"));
    }

    @BuildStep
    void runtimeInitialization(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitializedClass) {
        runtimeInitializedClass
                .produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.BlockSource$ThreadedCachingBlockSource"));
    }

}
