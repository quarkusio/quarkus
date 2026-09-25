package io.quarkus.gradle.application.internal.plugin;

import java.util.Optional;

import org.gradle.api.file.Directory;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskProvider;

import io.quarkus.gradle.application.dsl.QuarkusAotJarOutput;
import io.quarkus.gradle.application.internal.planning.TaskNames;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.tasks.QuarkusApplicationBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationImageBuildTask;
import io.quarkus.gradle.application.tasks.QuarkusApplicationIntegrationTestMetadataTask;

record QuarkusApplicationIntegrationTestBuild(
        String buildName,
        QuarkusApplicationBuildType buildType,
        TaskProvider<? extends QuarkusApplicationBuildTask> task,
        Provider<Directory> metadataDirectory,
        Provider<RegularFile> resultFile,
        Provider<Directory> launcherMetadataDirectory,
        Optional<TaskProvider<QuarkusApplicationIntegrationTestMetadataTask>> launcherMetadataTask,
        TaskNames taskNames,
        Optional<QuarkusAotJarOutput> aotJarOutput,
        Optional<TaskProvider<QuarkusApplicationImageBuildTask>> baseImageTask,
        Optional<Provider<RegularFile>> baseImageReceipt) {
}
