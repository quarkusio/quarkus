package io.quarkus.jdbc.hsqldb.deployment;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hsqldb.lib.ReadWriteLockDummy;
import org.hsqldb.types.Type;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.jdbc.hsqldb.runtime.HsqldbReflections;

/**
 * Registers the {@code org.hsqldb.jdbc.JDBCDriver} so that it can be loaded by reflection, as commonly expected.
 */
public final class HsqldbJdbcReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        final String driverName = "org.hsqldb.jdbc.JDBCDriver";
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(driverName).build());
    }

    @BuildStep
    NativeImageFeatureBuildItem enableHSQLDBFeature() {
        return new NativeImageFeatureBuildItem("io.quarkus.jdbc.hsqldb.runtime.HsqldbReflections");
    }

    /**
     * We need to index the HSQLDB database core jar so to include custom extension types it's
     * loading via reflection.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    IndexDependencyBuildItem indexHSQLDBLibrary() {
        return new IndexDependencyBuildItem("org.hsqldb", "hsqldb");
    }

    /**
     * All implementors of {@link StatefulDataType.Factory} might get loaded reflectively.
     * While we could hardcode the list included in HSQLDB, we prefer looking them up via Jandex
     * so to also load custom implementations optionally provided by user code.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    GeneratedResourceBuildItem listStatefulDataTypeFactories(CombinedIndexBuildItem index) {
        return generateListBy(HsqldbReflections.REZ_NAME_STATEFUL_DATATYPES, index,
                // TODO: This needs analysis of reflection in HSQLDB sources. Using a random class for now.
                i -> i.getAllKnownImplementors(ReadWriteLockDummy.class).stream());
    }

    /**
     * All implementors of {@link DataType} which have an INSTANCE field
     * need this field to be accessible via reflection.
     * While we could hardcode the list included in HSQLDB, we prefer looking them up via Jandex
     * so to also load custom implementations optionally provided by user code.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    GeneratedResourceBuildItem listBasicDataTypes(CombinedIndexBuildItem index) {
        return generateListBy(HsqldbReflections.REZ_NAME_DATA_TYPE_SINGLETONS, index,
                // TODO: This needs analysis of reflection in HSQLDB sources. Using a random class for now.
                i -> i.getAllKnownImplementors(org.hsqldb.types.Type.class)
                        .stream().filter(classInfo -> classInfo.field("INSTANCE") != null));
    }

    private static GeneratedResourceBuildItem generateListBy(String resourceName, CombinedIndexBuildItem index,
            Function<IndexView, Stream<ClassInfo>> selection) {
        String classNames = selection.apply(index.getIndex()).map(ClassInfo::name).map(DotName::toString).sorted()
                .collect(Collectors.joining("\n"));
        return new GeneratedResourceBuildItem(resourceName, classNames.getBytes(StandardCharsets.UTF_8));
    }

}