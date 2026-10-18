package io.quarkus.jdbc.h2.deployment;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.h2.mvstore.type.DataType;
import org.h2.mvstore.type.StatefulDataType;
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
import io.quarkus.jdbc.h2.runtime.H2Reflections;

/**
 * Registers the {@code org.h2.Driver} so that it can be loaded
 * by reflection, as commonly expected.
 *
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class H2JDBCReflections {

    @BuildStep
    void build(BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        //Not strictly necessary when using Agroal, as it also registers
        //any JDBC driver being configured explicitly through its configuration.
        //We register it for the sake of people not using Agroal.
        final String driverName = "org.h2.Driver";
        reflectiveClass.produce(ReflectiveClassBuildItem.builder(driverName).build());
    }

    @BuildStep
    NativeImageFeatureBuildItem enableH2Feature() {
        return new NativeImageFeatureBuildItem("io.quarkus.jdbc.h2.runtime.H2Reflections");
    }

    /**
     * We need to index the H2 database core jar so to include custom extension types it's
     * loading via reflection.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    IndexDependencyBuildItem indexH2Library() {
        return new IndexDependencyBuildItem("com.h2database", "h2");
    }

    /**
     * All implementors of {@link StatefulDataType.Factory} might get loaded reflectively.
     * While we could hardcode the list included in H2, we prefer looking them up via Jandex
     * so to also load custom implementations optionally provided by user code.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    GeneratedResourceBuildItem listStatefulDataTypeFactories(CombinedIndexBuildItem index) {
        return generateListBy(H2Reflections.REZ_NAME_STATEFUL_DATATYPES, index,
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
        return generateListBy(H2Reflections.REZ_NAME_DATA_TYPE_SINGLETONS, index,
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
