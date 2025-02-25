package io.quarkus.jdbc.oracle.deployment;

import java.util.Collections;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ExcludeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageAllowIncompleteClasspathBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.maven.dependency.ArtifactKey;

/**
 * The Oracle JDBC driver includes a {@literal META-INF/native-image} which enables a set
 * of global flags we need to control better, so to ensure such flags do not interfere
 * with requirements of other libraries.
 * <p>
 * For this reason, the {@literal META-INF/native-image/native-image.properties} resource
 * is excluded explicitly; then we re-implement the equivalent directives using Quarkus
 * build items.
 * <p>
 * Other resources such as {@literal jni-config.json} and {@literal resource-config.json}
 * are not excluded, so to ensure we match the recommendations from the Oracle JDBC
 * engineering team and make it easier to pick up improvements in these when the driver
 * gets updated.
 * <p>
 * Regarding {@literal reflect-config.json}, we also prefer excluding it for the time
 * being even though it's strictly not necessary: the reason is that the previous driver
 * version had a build-breaking mistake; this was fixed in version 21.3 so should no
 * longer be necessary, but the previous driver had been tested more widely and would
 * require it, so this would facilitate the option to revert to the older version in
 * case of problems.
 */
@BuildSteps(onlyIf = NativeOrNativeSourcesBuild.class)
public final class OracleMetadataOverrides {

    static final String DRIVER_JAR_MATCH_REGEX = "com\\.oracle\\.database\\.jdbc";
    static final String NATIVE_IMAGE_RESOURCE_MATCH_REGEX = "/META-INF/native-image/native-image\\.properties";
    static final String NATIVE_IMAGE_REFLECT_CONFIG_MATCH_REGEX = "/META-INF/native-image/reflect-config\\.json";

    /**
     * Should match the contents of {@literal reflect-config.json}
     *
     * @param reflectiveClass buildItem producer
     */
    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //This is to match the Oracle metadata (which we excluded so that we can apply fixes):
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.jdbc.internal.ACProxyable")
                .constructors().methods().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.jdbc.driver.T4CDriverExtension")
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.jdbc.driver.T2CDriverExtension")
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.jdbc.driver.ShardingDriverExtension")
                .build());
        reflectiveClass.produce(
                ReflectiveClassBuildItem.builder("oracle.net.ano.Ano").build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.net.ano.AuthenticationService")
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.net.ano.DataIntegrityService")
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.net.ano.EncryptionService")
                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.net.ano.SupervisorService")
                .build());
        //This is listed in the original metadata, but it doesn't actually exist:
        //        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.jdbc.driver.Message11")
        //                .build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.sql.TypeDescriptor")
                .constructors().fields().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.sql.TypeDescriptorFactory")
                .constructors().build());
        reflectiveClass.produce(ReflectiveClassBuildItem.builder("oracle.sql.AnyDataFactory")
                .constructors().build());
    }

    @BuildStep
    void runtimeInitializeDriver(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized,
            BuildProducer<RuntimeReinitializedClassBuildItem> runtimeReinitialized) {
        //These re-implement all the "--initialize-at-build-time" arguments found in the native-image.properties :

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
        runtimeReinitialized.produce(new RuntimeReinitializedClassBuildItem(
                "oracle.jdbc.driver.BlockSource$ThreadedCachingBlockSource"));
        runtimeReinitialized.produce(new RuntimeReinitializedClassBuildItem(
                "oracle.jdbc.driver.BlockSource$ThreadedCachingBlockSource$BlockReleaser"));

        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.xa.client.OracleXADataSource"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.replay.OracleXADataSourceImpl"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.datasource.OracleXAConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.xa.client.OracleXAConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.xa.client.OracleXAHeteroConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.jdbc.driver.T4CXAConnection"));
        runtimeInitialized.produce(new RuntimeInitializedClassBuildItem("oracle.security.o5logon.O5Logon"));

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
    void excludeOracleDirectives(BuildProducer<ExcludeConfigBuildItem> nativeImageExclusions) {
        // Excludes both native-image.properties and reflect-config.json, which are reimplemented above.
        // N.B. this could be expressed by using a single regex to match both resources,
        // but such a regex would include a ? char, which breaks arguments parsing on Windows.
        nativeImageExclusions.produce(new ExcludeConfigBuildItem(DRIVER_JAR_MATCH_REGEX, NATIVE_IMAGE_RESOURCE_MATCH_REGEX));
        nativeImageExclusions
                .produce(new ExcludeConfigBuildItem(DRIVER_JAR_MATCH_REGEX, NATIVE_IMAGE_REFLECT_CONFIG_MATCH_REGEX));
    }

    @BuildStep
    NativeImageAllowIncompleteClasspathBuildItem naughtyDriver() {
        return new NativeImageAllowIncompleteClasspathBuildItem("quarkus-jdbc-oracle");
    }

    @BuildStep
    RemovedResourceBuildItem enhancedCharsetSubstitutions() {
        return new RemovedResourceBuildItem(ArtifactKey.fromString("com.oracle.database.jdbc:ojdbc17"),
                Collections.singleton("oracle/nativeimage/CharacterSetFeature.class"));
    }

    @BuildStep
    NativeImageResourceBundleBuildItem additionalResourceBundles() {
        return new NativeImageResourceBundleBuildItem("oracle.net.mesg.NetErrorMessages");
    }

}
