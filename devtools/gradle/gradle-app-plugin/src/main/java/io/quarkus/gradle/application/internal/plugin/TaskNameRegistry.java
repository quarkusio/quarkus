package io.quarkus.gradle.application.internal.plugin;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

final class TaskNameRegistry {

    private final Set<String> taskNames = new HashSet<>();

    void register(Project project, String taskName) {
        String key = taskName.toLowerCase(Locale.ROOT);
        if (!taskNames.add(key) || project.getTasks().getNames().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(key::equals)) {
            throw new GradleException("Quarkus application task name '" + taskName + "' collides with an existing task");
        }
    }
}
