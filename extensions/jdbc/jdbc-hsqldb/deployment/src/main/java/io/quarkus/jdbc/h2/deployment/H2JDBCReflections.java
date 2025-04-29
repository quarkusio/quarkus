package io.quarkus.jdbc.hsqldb.deployment;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hsqldb.mvstore.type.DataType;
import org.hsqldb.mvstore.type.StatefulDataType;
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
import io.quarkus.jdbc.hsqldb.runtime.HSQLDBReflections;

/**
 * Registers the {@code org.hsqldb.Driver} so that it can be loaded
 * by reflection, as commonly expected.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class HSQLDBJDBCReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        final String driverName = "org.hsqldb.Driver";
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(driverName).build());
    }

    @BuildStep
    NativeImageFeatureBuildItem enableHSQLDBFeature() {
        return new NativeImageFeatureBuildItem("io.quarkus.jdbc.hsqldb.runtime.HSQLDBReflections");
    }

    /**
     * We need to index the HSQLDB database core jar so to include custom extension types it's
     * loading via reflection.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    IndexDependencyBuildItem indexHSQLDBLibrary() {
        return new IndexDependencyBuildItem("com.hsqldbdatabase", "hsqldb");
    }

    /**
     * All implementors of {@link StatefulDataType.Factory} might get loaded reflectively.
     * While we could hardcode the list included in H2, we prefer looking them up via Jandex
     * so to also load custom implementations optionally provided by user code.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    GeneratedResourceBuildItem listStatefulDataTypeFactories(CombinedIndexBuildItem index) {
        return generateListBy(HSQLDBReflections.REZ_NAME_STATEFUL_DATATYPES, index,
                i -> i.getAllKnownImplementors(StatefulDataType.Factory.class).stream());
    }

    /**
     * All implementors of {@link DataType} which have an INSTANCE field
     * need this field to be accessible via reflection.
     * While we could hardcode the list included in H2, we prefer looking them up via Jandex
     * so to also load custom implementations optionally provided by user code.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    GeneratedResourceBuildItem listBasicDataTypes(CombinedIndexBuildItem index) {
        return generateListBy(HSQLDBReflections.REZ_NAME_DATA_TYPE_SINGLETONS, index,
                i -> i.getAllKnownImplementors(DataType.class)
                        .stream().filter(classInfo -> classInfo.field("INSTANCE") != null));
    }

    private static GeneratedResourceBuildItem generateListBy(String resourceName, CombinedIndexBuildItem index,
            Function<IndexView, Stream<ClassInfo>> selection) {
        String classNames = selection.apply(index.getIndex()).map(ClassInfo::name).map(DotName::toString).sorted()
                .collect(Collectors.joining("\n"));
        return new GeneratedResourceBuildItem(resourceName, classNames.getBytes(StandardCharsets.UTF_8));
    }

}