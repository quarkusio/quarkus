package io.quarkus.vertx.core.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.NativeImageConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.vertx.core.file.FileSystemOptions;

public class FileSystemOptionsNativeConfigTest {

    @Test
    void fileSystemOptionsIsRuntimeInitialized() {
        List<ReflectiveClassBuildItem> reflectiveClasses = new ArrayList<>();
        List<NativeImageResourceBuildItem> nativeImageResources = new ArrayList<>();

        BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer = reflectiveClasses::add;
        BuildProducer<NativeImageResourceBuildItem> nativeImageResourceProducer = nativeImageResources::add;

        NativeImageConfigBuildItem config = new VertxCoreProcessor().build(
                reflectiveClassProducer,
                nativeImageResourceProducer);

        assertThat(config.getRuntimeInitializedClasses())
                .contains(FileSystemOptions.class.getName());
    }

}
