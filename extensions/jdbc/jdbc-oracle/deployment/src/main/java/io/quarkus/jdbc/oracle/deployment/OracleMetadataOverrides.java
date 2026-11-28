package io.quarkus.jdbc.oracle.deployment;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAllowIncompleteClasspathBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;

/**
 * The Oracle JDBC driver ships its own native-image metadata under
 * {@literal META-INF/native-image/com.oracle.database.jdbc/ojdbc17}: it registers the reflection, resources and
 * resource bundles the driver needs, and enables its {@code NativeImageFeature}.
 * <p>
 * The build steps below add what that metadata does not cover: the classes that must be initialized at run time.
 */
@BuildSteps
public final class OracleMetadataOverrides {

    @BuildStep
    void runtimeInitializeDriver(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized,
            BuildProducer<RuntimeInitializedClassBuildItem> runtimeReinitialized) {
        //These come from the "--initialize-at-run-time" arguments that older drivers had in their native-image.properties:

        // Override: the original metadata marks the drivers as "runtime initialized" but this makes it incompatible with
        // other systems (e.g. DB2 drivers) as it makes them incompatible with the JDK DriverManager integrations:
        // the DriverManager will typically (and most likely) need to load all drivers in a different phase.
        // runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.OracleDriver"));
        // runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.OracleDriver"));

        // The Oracle driver's metadata hints to require java.sql.DriverManager to be initialized at runtime, but:
        //  A) I disagree with the fact that a driver makes changes outside its scope (java.sql in this case)
        //  B) It does actually not compile if you have other JDBC drivers, as other implementations need this class initialized during build
        //  C) This metadata is expected to get improved in the next public release of the Oracle JDBC driver
        // runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("java.sql.DriverManager"));

        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.LogicalConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.pool.OraclePooledConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.pool.OracleDataSource"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.datasource.impl.OracleDataSource"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.pool.OracleOCIConnectionPool"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.OracleTimeoutThreadPerVM"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.net.nt.TimeoutInterruptHandler"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.HAManager"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.net.nt.Clock"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.net.nt.TcpMultiplexer"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.net.nt.TcpMultiplexer"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.net.nt.TcpMultiplexer$LazyHolder"));

        // Needs to be REinitialized to avoid problems when also using the Elasticsearch Java client
        // See https://github.com/quarkusio/quarkus/issues/31624#issuecomment-1457706253
        runtimeReinitialized.produce(new RuntimeInitializedClassBuildItem(
                "oracle.jdbc.driver.BlockSource$ThreadedCachingBlockSource"));
        runtimeReinitialized.produce(new RuntimeInitializedClassBuildItem(
                "oracle.jdbc.driver.BlockSource$ThreadedCachingBlockSource$BlockReleaser"));

        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.xa.client.OracleXADataSource"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.replay.OracleXADataSourceImpl"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.datasource.OracleXAConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.xa.client.OracleXAConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.xa.client.OracleXAHeteroConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.T4CXAConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.security.o5logon.O5Logon"));

        // These two classes, new in driver version 23.26.3.0.0, hold a static field of type
        // SSLContext, eagerly initialized in a static initializer; such an object cannot be
        // initialized at build time.
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.net.aso.TLSHandshakeHandler"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.net.nt.EZTLSKeyVerifier"));

        //These were missing in the original driver, and apparently in its automatic feature definitions as well;
        //the need was spotted by running the native build: GraalVM will complain about these types having initialized fields
        //referring to various other types which aren't allowed in a captured heap.
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.diagnostics.Diagnostic"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.replay.driver.FailoverManagerImpl"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.diagnostics.AbstractDiagnosable"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.AbstractTrueCacheConnectionPools"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.diagnostics.CommonDiagnosable"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.replay.driver.TxnFailoverManagerImpl"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.diagnostics.OracleDiagnosticsMXBean"));
    }

    @BuildStep
    NativeImageAllowIncompleteClasspathBuildItem naughtyDriver() {
        return new NativeImageAllowIncompleteClasspathBuildItem("quarkus-jdbc-oracle");
    }
}
