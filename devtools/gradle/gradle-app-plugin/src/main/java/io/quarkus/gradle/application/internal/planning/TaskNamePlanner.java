package io.quarkus.gradle.application.internal.planning;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationDeploymentDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationLaunchDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationLaunchKind;

public final class TaskNamePlanner {

    private static final Set<String> LEGACY_TASK_NAMES = Set.of("quarkusBuild", "imageBuild", "imagePush", "buildNative",
            "testNative", "deploy", "buildAotEnhancedImage");

    public TaskNames taskNames(QuarkusApplicationBuildDescriptor descriptor) {
        String prefix = "quarkus" + TaskNameSegment.of(descriptor.name()).value();
        return new TaskNames(
                prefix + "Build",
                prefix + "ShowEffectiveConfig",
                prefix + "Run",
                prefix + "ImageBuild",
                prefix + "ImagePush",
                prefix + "StartupArchiveValidation",
                prefix + "StartupOptimizedImageBuild",
                prefix + "StartupOptimizedImagePush",
                prefix + "NativeTest");
    }

    public String deployTaskName(QuarkusApplicationBuildDescriptor build,
            QuarkusApplicationDeploymentDescriptor deployment) {
        return "quarkus" + TaskNameSegment.of(build.name()).value() + "DeployTo"
                + TaskNameSegment.of(deployment.name()).value();
    }

    public String continuousTestTaskName(QuarkusApplicationLaunchDescriptor launch) {
        if (launch.kind() != QuarkusApplicationLaunchKind.CONTINUOUS_TEST) {
            throw new IllegalArgumentException("Only continuous-test launch descriptors derive a continuous-test task name");
        }
        return launch.name()
                .map(name -> "quarkus" + TaskNameSegment.of(name).value() + "ContinuousTest")
                .orElse("quarkusContinuousTest");
    }

    public void validateBuildNames(Collection<QuarkusApplicationBuildDescriptor> descriptors) {
        Map<String, String> seen = new HashMap<>();
        for (QuarkusApplicationBuildDescriptor descriptor : descriptors) {
            var name = TaskNameSegment.of(descriptor.name());
            String previous = seen.putIfAbsent(name.collisionKey(), name.name());
            if (previous != null) {
                throw new IllegalArgumentException("Quarkus application build names '" + previous + "' and '"
                        + name.name() + "' derive the same task-name segment");
            }
        }
    }

    public void validateTaskNameCollisions(Collection<QuarkusApplicationBuildDescriptor> descriptors,
            Collection<String> existingTaskNames) {
        Set<String> seen = new HashSet<>();
        Set<String> existing = new HashSet<>();
        existingTaskNames.forEach(name -> existing.add(key(name)));
        LEGACY_TASK_NAMES.forEach(name -> existing.add(key(name)));

        for (QuarkusApplicationBuildDescriptor descriptor : descriptors) {
            var names = taskNames(descriptor);
            validateTaskName(names.build(), descriptor.name(), seen, existing);
            validateTaskName(names.showEffectiveConfig(), descriptor.name(), seen, existing);
            if (descriptor.type().isJar()) {
                validateTaskName(names.run(), descriptor.name(), seen, existing);
            }
            validateTaskName(names.imageBuild(), descriptor.name(), seen, existing);
            validateTaskName(names.imagePush(), descriptor.name(), seen, existing);
            if (descriptor.type() == QuarkusApplicationBuildType.AOT_JAR) {
                validateTaskName(names.startupArchiveValidation(), descriptor.name(), seen, existing);
                validateTaskName(names.startupOptimizedImageBuild(), descriptor.name(), seen, existing);
                validateTaskName(names.startupOptimizedImagePush(), descriptor.name(), seen, existing);
            }
            if (descriptor.type() == QuarkusApplicationBuildType.NATIVE_EXECUTABLE) {
                validateTaskName(names.nativeTest(), descriptor.name(), seen, existing);
            }
        }
    }

    public void validateDeploymentNames(Collection<QuarkusApplicationDeploymentDescriptor> deployments) {
        Map<String, String> seen = new HashMap<>();
        for (QuarkusApplicationDeploymentDescriptor deployment : deployments) {
            var name = TaskNameSegment.of(deployment.name());
            String previous = seen.putIfAbsent(name.collisionKey(), name.name());
            if (previous != null) {
                throw new IllegalArgumentException("Quarkus application deployment names '" + previous + "' and '"
                        + name.name() + "' derive the same task-name segment");
            }
        }
    }

    private static void validateTaskName(String taskName, String buildName, Set<String> seen,
            Set<String> existing) {
        String key = key(taskName);
        if (existing.contains(key)) {
            throw new IllegalArgumentException("Quarkus application build '" + buildName
                    + "' derives task name '" + taskName + "', which collides with an existing task");
        }
        if (!seen.add(key)) {
            throw new IllegalArgumentException("Quarkus application build '" + buildName
                    + "' derives duplicate task name '" + taskName + "'");
        }
    }

    private static String key(String taskName) {
        return taskName.toLowerCase(Locale.ROOT);
    }
}
